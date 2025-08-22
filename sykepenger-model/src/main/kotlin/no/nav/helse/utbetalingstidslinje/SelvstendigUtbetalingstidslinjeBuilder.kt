package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
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

    internal fun result(sykdomstidslinje: Sykdomstidslinje): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                is Dag.Arbeidsdag -> arbeidsdag(builder, dag.dato)
                is Dag.ForeldetSykedag -> foreldetdag(builder, dag.dato, dag.grad)
                is Dag.FriskHelgedag -> arbeidsdag(builder, dag.dato)
                is Dag.SykHelgedag -> helg(builder, dag.dato, dag.grad)
                is Dag.Sykedag -> navDag(builder, dag.dato, dag.grad)
                is Dag.Ventetidsdag -> ventetidsdag(builder, dag.dato, dag.grad)

                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.AndreYtelser,
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
}
