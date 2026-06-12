package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.DagerUtenNavAnsvaravklaring
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

internal data class VentedagerForVedtaksperiode(
    val vedtaksperiode: Periode,
    val dagerUtenNavAnsvar: DagerUtenNavAnsvaravklaring,
    val dagerNavOvertarAnsvar: List<Periode>
) {
    val erUtenforAGP = dagerUtenNavAnsvar.ferdigAvklart && (dagerUtenNavAnsvar.periode == null || vedtaksperiode.endInclusive > dagerUtenNavAnsvar.periode.endInclusive)
    val navSkalOvertaAGP = dagerNavOvertarAnsvar.any { it.overlapperMed(vedtaksperiode) }

    val skalFatteVedtak = erUtenforAGP || navSkalOvertaAGP
}

internal interface UtbetalingstidslinjeBuilder {
    fun result(sykdomstidslinje: Sykdomstidslinje, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Utbetalingstidslinje
}

internal class ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
    private val arbeidsgiverperiode: List<Periode>,
    private val dagerNavOvertarAnsvar: List<Periode>,
    private val refusjonstidslinje: Beløpstidslinje,
    private val avslagstidslinje: Avslagstidslinje
) : UtbetalingstidslinjeBuilder {
    internal fun medInntektHvisFinnes(
        dato: LocalDate,
        grad: Prosentdel,
        fastsattÅrsinntekt: Inntekt,
        inntektjusteringer: Beløpstidslinje
    ): Økonomi {
        return medInntekt(dato, grad, fastsattÅrsinntekt, inntektjusteringer)
    }

    private fun medInntekt(dato: LocalDate, grad: Prosentdel, fastsattÅrsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Økonomi {
        return Økonomi.inntekt(
            sykdomsgrad = grad,
            aktuellDagsinntekt = fastsattÅrsinntekt,
            dekningsgrad = 100.prosent,
            refusjonsbeløp = (refusjonstidslinje[dato] as? Beløpsdag)?.beløp ?: INGEN,
            inntektjustering = (inntektjusteringer[dato] as? Beløpsdag)?.beløp ?: INGEN,
        )
    }

    private fun Utbetalingstidslinje.Builder.avslagsdagEller(dato: LocalDate, sykdomsgrad: Prosentdel, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje, eller: () -> Unit) = when (val avslagsdag = avslagstidslinje[dato]) {
        null -> eller()
        else -> avvistDag(this, dato, sykdomsgrad, avslagsdag.begrunnelser, inntekt, inntektjusteringer)
    }

    private fun Utbetalingstidslinje.Builder.avslagsdag(dato: LocalDate, sykdomsgrad: Prosentdel, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje, begrunnelser: List<Begrunnelse>) {
        check(begrunnelser.isNotEmpty()) { "Forventer at det finnes begrunnelser for avslagsdag" }
        val avslagsbegrunnelser = (avslagstidslinje[dato]?.begrunnelser ?: emptyList()) + begrunnelser
        avvistDag(this, dato, sykdomsgrad, avslagsbegrunnelser, inntekt, inntektjusteringer)
    }

    override fun result(sykdomstidslinje: Sykdomstidslinje, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                /** <potensielt arbeidsgiverperiode-dager> **/
                is Dag.ArbeidsgiverHelgedag -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    else helg(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                }

                is Dag.Arbeidsgiverdag -> {
                    if (erAGP(dag.dato)) builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                        arbeidsgiverperiodedag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    }
                    else builder.avslagsdag(dag.dato, dag.grad, inntekt, inntektjusteringer, listOf(Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode))
                }

                is Dag.Sykedag -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedagEllerNavAnsvar(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    else navDag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                }

                is Dag.SykHelgedag -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    else helg(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                }

                is Dag.AndreYtelser -> {
                    // andreytelse-dagen er fridag hvis den overlapper med en agp-dag, eller om vedtaksperioden ikke har noen agp -- fordi andre ytelsen spiser opp alt
                    if (erAGP(dag.dato)) {
                        builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                            arbeidsgiverperiodedag(builder, dag.dato, 0.prosent, inntekt, inntektjusteringer)
                        }
                    }
                    else if (arbeidsgiverperiode.isEmpty() || dag.dato < arbeidsgiverperiode.first().start) {
                        builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                            fridag(builder, dag.dato, inntekt, inntektjusteringer)
                        }
                    }
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
                        builder.avslagsdag(dag.dato, 0.prosent, inntekt, inntektjusteringer, listOf(begrunnelse))
                    }
                }

                is Dag.Feriedag -> builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, 0.prosent, inntekt, inntektjusteringer)
                    else fridag(builder, dag.dato, inntekt, inntektjusteringer)
                }

                is Dag.ForeldetSykedag -> builder.avslagsdagEller(dag.dato, dag.grad, inntekt, inntektjusteringer) {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.grad, inntekt, inntektjusteringer)
                    else builder.addForeldetDag(dag.dato, medInntektHvisFinnes(dag.dato, dag.grad, inntekt, inntektjusteringer).ikkeBetalt())
                }

                is Dag.ArbeidIkkeGjenopptattDag -> builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, 0.prosent, inntekt, inntektjusteringer)
                    else fridag(builder, dag.dato, inntekt, inntektjusteringer)
                }

                is Dag.Permisjonsdag -> builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, 0.prosent, inntekt, inntektjusteringer)
                    else fridag(builder, dag.dato, inntekt, inntektjusteringer)
                }
                /** </potensielt arbeidsgiverperiode-dager> **/

                is Dag.Arbeidsdag,
                is Dag.FriskHelgedag -> builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                    arbeidsdag(builder, dag.dato, inntekt, inntektjusteringer)
                }

                is Dag.ProblemDag -> builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                    // den andre builderen kaster egentlig exception her, men trenger vi det –– sånn egentlig?
                    fridag(builder, dag.dato, inntekt, inntektjusteringer)
                }

                is Dag.UkjentDag -> builder.avslagsdagEller(dag.dato, 0.prosent, inntekt, inntektjusteringer) {
                    // todo: pga strekking av egenmeldingsdager fra søknad så har vi vedtaksperioder med ukjentdager
                    // error("Forventer ikke å finne en ukjentdag i en vedtaksperiode")
                    when (dag.dato.erHelg()) {
                        true -> fridag(builder, dag.dato, inntekt, inntektjusteringer)
                        false -> arbeidsdag(builder, dag.dato, inntekt, inntektjusteringer)
                    }
                }

                is Dag.MeldingTilNavDag,
                is Dag.MeldingTilNavHelgedag -> error("Forventer ikke MeldingTilNavDag i arbeidsgiverperiodeberegningen")
            }
        }
        return builder.build()
    }

    private fun erAGP(dato: LocalDate) = arbeidsgiverperiode.any { dato in it }
    private fun erAGPNavAnsvar(dato: LocalDate) = dagerNavOvertarAnsvar.any { dato in it }

    private fun arbeidsgiverperiodedagEllerNavAnsvar(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, fastsattÅrsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        if (erAGPNavAnsvar(dato) && !dato.erHelg())
            return builder.addArbeidsgiverperiodedagNav(dato, medInntektHvisFinnes(dato, grad, fastsattÅrsinntekt, inntektjusteringer))
        builder.addArbeidsgiverperiodedag(dato, medInntektHvisFinnes(dato, grad, fastsattÅrsinntekt, inntektjusteringer).ikkeBetalt())
    }

    private fun arbeidsgiverperiodedag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, fastsattÅrsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addArbeidsgiverperiodedag(dato, medInntektHvisFinnes(dato, grad, fastsattÅrsinntekt, inntektjusteringer).ikkeBetalt())
    }

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, begrunnelser: List<Begrunnelse>, fastsattÅrsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(dato, grad, fastsattÅrsinntekt, inntektjusteringer).ikkeBetalt(), begrunnelser)
    }

    private fun helg(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, fastsattÅrsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addHelg(dato, medInntektHvisFinnes(dato, grad, fastsattÅrsinntekt, inntektjusteringer).ikkeBetalt())
    }

    private fun navDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, fastsattÅrsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addNAVdag(dato, medInntektHvisFinnes(dato, grad, fastsattÅrsinntekt, inntektjusteringer))
    }

    private fun fridag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, fastsattÅrsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addFridag(dato, medInntektHvisFinnes(dato, 0.prosent, fastsattÅrsinntekt, inntektjusteringer).ikkeBetalt())
    }

    private fun arbeidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, fastsattÅrsinntekt: Inntekt, inntektjusteringer: Beløpstidslinje) {
        builder.addArbeidsdag(dato, medInntektHvisFinnes(dato, 0.prosent, fastsattÅrsinntekt, inntektjusteringer).ikkeBetalt())
    }
}
