package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

internal data class ArbeidsgiverperiodeForVedtaksperiode(
    val vedtaksperiode: Periode,
    val arbeidsgiverperioder: List<Periode>
)

internal class UtbetalingstidslinjeBuilderVedtaksperiode(
    private val regler: ArbeidsgiverRegler,
    private val arbeidsgiverperiode: List<Periode>,
    private val dagerNavOvertarAnsvar: List<Periode>,
    private val refusjonstidslinje: Beløpstidslinje,
    private val inntektstidslinje: Beløpstidslinje
) {

    private val lagDefaultRefusjonsbeløpHvisMangler = { _: LocalDate, aktuellDagsinntekt: Inntekt -> aktuellDagsinntekt }
    private val krevRefusjonsbeløpHvisMangler = { dato: LocalDate, _: Inntekt ->
        error("Har ingen refusjonsopplysninger på vilkårsgrunnlag for utbetalingsdag $dato")
    }

    private fun refusjonsbeløp(dato: LocalDate, aktuellDagsinntekt: Inntekt, refusjonsopplysningFinnesIkkeStrategi: (LocalDate, Inntekt) -> Inntekt): Inntekt {
        val refusjonFraBehandling = refusjonstidslinje[dato].takeIf { it is Beløpsdag }?.beløp
        return refusjonFraBehandling ?: refusjonsopplysningFinnesIkkeStrategi(dato, aktuellDagsinntekt)
    }

    internal fun medInntektHvisFinnes(
        dato: LocalDate,
        grad: Prosentdel
    ): Økonomi {
        return medInntekt(dato, grad, lagDefaultRefusjonsbeløpHvisMangler)
    }

    private fun medInntektOrThrow(
        dato: LocalDate,
        grad: Prosentdel
    ): Økonomi {
        return medInntekt(dato, grad, krevRefusjonsbeløpHvisMangler)
    }

    private fun medInntekt(
        dato: LocalDate,
        grad: Prosentdel,
        refusjonsopplysningFinnesIkkeStrategi: (LocalDate, Inntekt) -> Inntekt
    ): Økonomi {
        val aktuellDagsinntekt = inntektstidslinje[dato].beløp
        return Økonomi.inntekt(
            grad = grad,
            aktuellDagsinntekt = aktuellDagsinntekt,
            dekningsgrunnlag = aktuellDagsinntekt * regler.dekningsgrad(),
            refusjonsbeløp = refusjonsbeløp(dato, aktuellDagsinntekt, refusjonsopplysningFinnesIkkeStrategi)
        )
    }

    internal fun result(sykdomstidslinje: Sykdomstidslinje): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                /** <potensielt arbeidsgiverperiode-dager> **/
                is Dag.ArbeidsgiverHelgedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.grad)
                    else helg(builder, dag.dato, dag.grad)
                }

                is Dag.Arbeidsgiverdag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedagEllerNavAnsvar(builder, dag.dato, dag.grad)
                    else avvistDag(builder, dag.dato, dag.grad, Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode)
                }

                is Dag.Sykedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedagEllerNavAnsvar(builder, dag.dato, dag.grad)
                    else navDag(builder, dag.dato, dag.grad)
                }

                is Dag.SykHelgedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.grad)
                    else helg(builder, dag.dato, dag.grad)
                }

                is Dag.AndreYtelser -> {
                    // andreytelse-dagen er fridag hvis den overlapper med en agp-dag, eller om vedtaksperioden ikke har noen agp -- fordi andre ytelsen spiser opp alt
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, 0.prosent)
                    else if (arbeidsgiverperiode.isEmpty() || dag.dato < arbeidsgiverperiode.first().start) fridag(builder, dag.dato)
                    else {
                        val begrunnelse = when (dag.ytelse) {
                            Dag.AndreYtelser.AnnenYtelse.AAP -> Begrunnelse.AndreYtelserAap
                            Dag.AndreYtelser.AnnenYtelse.Dagpenger -> Begrunnelse.AndreYtelserDagpenger
                            Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
                            Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
                            Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
                            Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> Begrunnelse.AndreYtelserPleiepenger
                            Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger
                        }
                        avvistDag(builder, dag.dato, 0.prosent, begrunnelse)
                    }
                }

                is Dag.Feriedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, 0.prosent)
                    else fridag(builder, dag.dato)
                }

                is Dag.ForeldetSykedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.grad)
                    else builder.addForeldetDag(dag.dato, medInntektHvisFinnes(dag.dato, dag.grad))
                }

                is Dag.ArbeidIkkeGjenopptattDag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, 0.prosent)
                    else fridag(builder, dag.dato)
                }

                is Dag.Permisjonsdag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, 0.prosent)
                    else fridag(builder, dag.dato)
                }
                /** </potensielt arbeidsgiverperiode-dager> **/

                is Dag.Arbeidsdag -> arbeidsdag(builder, dag.dato)
                is Dag.FriskHelgedag -> arbeidsdag(builder, dag.dato)
                is Dag.ProblemDag -> {
                    // den andre builderen kaster egentlig exception her, men trenger vi det –– sånn egentlig?
                    fridag(builder, dag.dato)
                }

                is Dag.UkjentDag -> {
                    // todo: pga strekking av egenmeldingsdager fra søknad så har vi vedtaksperioder med ukjentdager
                    // error("Forventer ikke å finne en ukjentdag i en vedtaksperiode")
                    when (dag.dato.erHelg()) {
                        true -> fridag(builder, dag.dato)
                        false -> arbeidsdag(builder, dag.dato)
                    }
                }
            }
        }
        return builder.build()
    }

    private fun erAGP(dato: LocalDate) = arbeidsgiverperiode.any { dato in it }
    private fun erAGPNavAnsvar(dato: LocalDate) = dagerNavOvertarAnsvar.any { dato in it }

    private fun arbeidsgiverperiodedagEllerNavAnsvar(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel) {
        if (erAGPNavAnsvar(dato) && !dato.erHelg())
            return builder.addArbeidsgiverperiodedagNav(dato, medInntektOrThrow(dato, grad))
        builder.addArbeidsgiverperiodedag(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt())
    }

    private fun arbeidsgiverperiodedag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel) {
        builder.addArbeidsgiverperiodedag(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt())
    }

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, begrunnelse: Begrunnelse) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt(), listOf(begrunnelse))
    }

    private fun helg(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel) {
        builder.addHelg(dato, medInntektHvisFinnes(dato, grad).ikkeBetalt())
    }

    private fun navDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel) {
        builder.addNAVdag(dato, medInntektOrThrow(dato, grad))
    }

    private fun fridag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        builder.addFridag(dato, medInntektHvisFinnes(dato, 0.prosent).ikkeBetalt())
    }

    private fun arbeidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        builder.addArbeidsdag(dato, medInntektHvisFinnes(dato, 0.prosent).ikkeBetalt())
    }
}
