package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class ArbeidsgiverUtbetalingstidslinje {

    private val utbetalingsdager = mutableListOf<Utbetalingsdag>()
    private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()
    private var helseState: HelseState = HelseState.IkkeSyk

    fun utbetalingslinjer(arbeidsgiverutbetalingstidslinjer: List<ArbeidsgiverUtbetalingstidslinje>) =
        this
            .limit(arbeidsgiverutbetalingstidslinjer)
            .filterByMinimumInntekt(arbeidsgiverutbetalingstidslinjer)
            .reduserAvSykdomsgrad(arbeidsgiverutbetalingstidslinjer)
            .utbetalingslinjer()

    private fun filterByMinimumInntekt(arbeidsgiverutbetalingstidslinjer: List<ArbeidsgiverUtbetalingstidslinje>) = this

    private fun reduserAvSykdomsgrad(arbeidsgiverutbetalingstidslinjer: List<ArbeidsgiverUtbetalingstidslinje>) = this

    private fun limit(arbeidsgiverutbetalingstidslinjer: List<ArbeidsgiverUtbetalingstidslinje>) = this

    private fun utbetalingslinjer(): List<Utbetalingslinje> {
        utbetalingsdager.forEach { it.accept(this, this.helseState) }
        return utbetalingslinjer
    }

    internal fun addArbeidsgiverperiodedag(inntekt: Double, dato: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.ArbeidsgiverperiodeDag(inntekt.roundToInt(), dato))
    }

    internal fun addNAVdag(inntekt: Double, dato: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.NavDag(inntekt.roundToInt(), dato))
    }

    internal fun addArbeidsdag(dagen: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.Arbeidsdag(dagen))
    }

    internal fun addFridag(dagen: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.Fridag(dagen))
    }

    private sealed class Utbetalingsdag(private val inntekt: Int, private val dato: LocalDate) {

        abstract fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState)

        internal class ArbeidsgiverperiodeDag(private val inntekt: Int, private val dato: LocalDate) :
            Utbetalingsdag(inntekt, dato) {
            override fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState) {
                state.visitArbeidsgiverperiodeDag(arbeidsgiverUtbetalingstidslinje, this)
            }
        }

        internal class NavDag(private val inntekt: Int, private val dato: LocalDate) :
            Utbetalingsdag(inntekt, dato) {
            override fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState) {
                state.visitNAVDag(arbeidsgiverUtbetalingstidslinje, this)
            }

            fun utbetalingslinje() = Utbetalingslinje(dato, dato, inntekt)
            fun oppdater(last: Utbetalingslinje) {
                last.tom = dato
            }
        }

        internal class Arbeidsdag(private val dato: LocalDate) :
            Utbetalingsdag(0, dato) {
            override fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState) {
                state.visitArbeidsdag(arbeidsgiverUtbetalingstidslinje, this)
            }
        }

        internal class Fridag(private val dato: LocalDate) :
            Utbetalingsdag(0, dato) {
            override fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState) {
                state.visitFridag(arbeidsgiverUtbetalingstidslinje, this)
            }
        }
    }

    private sealed class HelseState {

        open fun visitArbeidsgiverperiodeDag(
            arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
            arbeidsgiverperiodeDag: Utbetalingsdag.ArbeidsgiverperiodeDag
        ) {
        }

        abstract fun visitNAVDag(
            arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
            NavDag: Utbetalingsdag.NavDag
        )

        open fun visitArbeidsdag(
            arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
            arbeidsdag: Utbetalingsdag.Arbeidsdag
        ) {
        }

        open fun visitFridag(
            arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
            fridag: Utbetalingsdag.Fridag
        ) {
        }

        internal object IkkeSyk : HelseState() {
            override fun visitNAVDag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                navDag: Utbetalingsdag.NavDag
            ) {
                arbeidsgiverUtbetalingstidslinje.utbetalingslinjer.add(navDag.utbetalingslinje())
                arbeidsgiverUtbetalingstidslinje.helseState = Syk
            }
        }

        internal object Syk : HelseState() {
            override fun visitNAVDag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                navDag: Utbetalingsdag.NavDag
            ) {
                navDag.oppdater(arbeidsgiverUtbetalingstidslinje.utbetalingslinjer.last())
            }

            override fun visitArbeidsgiverperiodeDag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                arbeidsgiverperiodeDag: Utbetalingsdag.ArbeidsgiverperiodeDag
            ) {
                arbeidsgiverUtbetalingstidslinje.helseState = IkkeSyk
            }

            override fun visitArbeidsdag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                arbeidsdag: Utbetalingsdag.Arbeidsdag
            ) {
                arbeidsgiverUtbetalingstidslinje.helseState = IkkeSyk
            }

            override fun visitFridag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                fridag: Utbetalingsdag.Fridag
            ) {
                arbeidsgiverUtbetalingstidslinje.helseState = IkkeSyk
            }
        }
    }

}
