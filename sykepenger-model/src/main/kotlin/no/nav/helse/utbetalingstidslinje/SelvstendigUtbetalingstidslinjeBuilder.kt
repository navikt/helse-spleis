package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

internal class SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
    private val fastsattÅrsinntekt: Inntekt
) {
    private fun medInntektHvisFinnes(grad: Prosentdel): Økonomi {
        return medInntekt(grad)
    }

    private fun medInntekt(grad: Prosentdel): Økonomi {
        return Økonomi.inntekt(
            sykdomsgrad = grad,
            aktuellDagsinntekt = fastsattÅrsinntekt,
            dekningsgrad = 80.prosent,
            refusjonsbeløp = INGEN,
            inntektjustering = INGEN,
        )
    }

    internal fun result(sykdomstidslinje: Sykdomstidslinje, ventetid: Periode): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                is Dag.Arbeidsdag -> arbeidsdag(builder, dag.dato)
                is Dag.ForeldetSykedag -> foreldetdag(builder, dag.dato, dag.grad)
                is Dag.FriskHelgedag -> arbeidsdag(builder, dag.dato)
                is Dag.SykHelgedag -> if (dag.dato in ventetid) ventetidsdag(builder, dag.dato, dag.grad) else helg(builder, dag.dato, dag.grad)
                is Dag.Sykedag -> if (dag.dato in ventetid) ventetidsdag(builder, dag.dato, dag.grad) else navDag(builder, dag.dato, dag.grad)
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
                    avvistDag(builder, dag.dato, 0.prosent, begrunnelse)
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

    private fun helg(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel) {
        builder.addHelg(dato, medInntektHvisFinnes(grad).ikkeBetalt())
    }

    private fun navDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel) {
        builder.addNAVdag(dato, medInntektHvisFinnes(grad))
    }

    private fun arbeidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        builder.addArbeidsdag(dato, medInntektHvisFinnes(0.prosent).ikkeBetalt())
    }

    private fun ventetidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, sykdomsgrad: Prosentdel) {
        builder.addVentetidsdag(dato, medInntektHvisFinnes(sykdomsgrad).ikkeBetalt())
    }

    private fun foreldetdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, sykdomsgrad: Prosentdel) {
        builder.addForeldetDag(dato, medInntektHvisFinnes(sykdomsgrad).ikkeBetalt())
    }

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, grad: Prosentdel, begrunnelse: Begrunnelse) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(grad).ikkeBetalt(), listOf(begrunnelse))
    }
}
