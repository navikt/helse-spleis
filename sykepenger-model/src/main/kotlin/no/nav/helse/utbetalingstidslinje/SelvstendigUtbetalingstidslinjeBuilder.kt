package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

internal class SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
    private val dekningsgrad: Prosentdel,
    private val ventetid: Periode,
    private val dagerNavOvertarAnsvar: List<Periode>
) : UtbetalingstidslinjeBuilder {
    private fun medInntektHvisFinnes(grad: Prosentdel, næringsinntekt: Inntekt): Økonomi {
        return medInntekt(grad, næringsinntekt)
    }

    private fun medInntekt(grad: Prosentdel, næringsinntekt: Inntekt): Økonomi {
        return Økonomi.inntekt(
            sykdomsgrad = grad,
            aktuellDagsinntekt = næringsinntekt,
            dekningsgrad = dekningsgrad,
            refusjonsbeløp = INGEN,
            inntektjustering = INGEN,
        )
    }

    override fun result(sykdomstidslinje: Sykdomstidslinje, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                is Dag.Arbeidsdag -> arbeidsdag(builder, dag.dato, inntekt)
                is Dag.ForeldetSykedag -> foreldetdag(builder, dag.dato, dag.grad, inntekt)
                is Dag.FriskHelgedag -> arbeidsdag(builder, dag.dato, inntekt)
                is Dag.SykHelgedag ->
                    if (dag.dato in ventetid)
                        ventetidsdag(builder, dag.dato, dag.grad, inntekt, false)
                    else
                        helg(builder, dag.dato, dag.grad, inntekt)
                is Dag.Sykedag ->
                    if (dag.dato in ventetid)
                        ventetidsdag(builder, dag.dato, dag.grad, inntekt, navSkalUtbetaleVentetidsDag(dag.dato))
                    else
                        navDag(builder, dag.dato, dag.grad, inntekt)
                is Dag.AndreYtelser -> {
                    val begrunnelse = when (dag.ytelse) {
                        Dag.AndreYtelser.AnnenYtelse.AAP -> Begrunnelse.AndreYtelserAap
                        Dag.AndreYtelser.AnnenYtelse.Dagpenger -> Begrunnelse.AndreYtelserDagpenger
                        Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
                        Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
                        Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
                        Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> Begrunnelse.AndreYtelserPleiepenger
                        Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger
                    }
                    avvistDag(builder, dag.dato, 0.prosent, begrunnelse, inntekt)
                }

                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.Arbeidsgiverdag,
                is Dag.ArbeidsgiverHelgedag,
                is Dag.Permisjonsdag,
                is Dag.ProblemDag,
                is Dag.Feriedag,
                is Dag.UkjentDag -> error("Forventer ikke ${dag::class.simpleName} i utbetalingstidslinjen for selvstendig næringsdrivende")
            }
        }
        return builder.build()
    }

    private fun navSkalUtbetaleVentetidsDag(dato: LocalDate): Boolean {
        return dagerNavOvertarAnsvar.any { it.contains(dato) }
    }

    private fun helg(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, næringsinntekt: Inntekt) {
        builder.addHelg(dato, medInntektHvisFinnes(grad, næringsinntekt).ikkeBetalt())
    }

    private fun navDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, næringsinntekt: Inntekt) {
        builder.addNAVdag(dato, medInntektHvisFinnes(grad, næringsinntekt))
    }

    private fun arbeidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, næringsinntekt: Inntekt) {
        builder.addArbeidsdag(dato, medInntektHvisFinnes(0.prosent, næringsinntekt).ikkeBetalt())
    }

    private fun ventetidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, sykdomsgrad: Prosentdel, næringsinntekt: Inntekt, skalUtbetales: Boolean) {
        if (skalUtbetales) {
            builder.addVentetidsdag(dato, medInntektHvisFinnes(sykdomsgrad, næringsinntekt))
        } else {
            builder.addVentetidsdag(dato, medInntektHvisFinnes(sykdomsgrad, næringsinntekt).ikkeBetalt())
        }
    }

    private fun foreldetdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, sykdomsgrad: Prosentdel, næringsinntekt: Inntekt) {
        builder.addForeldetDag(dato, medInntektHvisFinnes(sykdomsgrad, næringsinntekt).ikkeBetalt())
    }

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, begrunnelse: Begrunnelse, næringsinntekt: Inntekt) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(grad, næringsinntekt).ikkeBetalt(), listOf(begrunnelse))
    }
}
