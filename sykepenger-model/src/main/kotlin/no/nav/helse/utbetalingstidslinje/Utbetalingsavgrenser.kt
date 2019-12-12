package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal class Utbetalingsavgrenser(private val tidslinje: ArbeidsgiverUtbetalingstidslinje,
                                    private val alderRegler: AlderRegler):
    ArbeidsgiverUtbetalingstidslinje.UtbetalingsdagVisitor() {
    private var state: State = State.Initiell
    private var betalteDager = 0
    private val ubetalteDager = mutableListOf<LocalDate>()

    internal fun ubetalteDager(): List<LocalDate> {
        tidslinje.accept(this)
        return ubetalteDager
    }

    private fun state(nyState: State) {
        state.leaving()
        state = nyState
        state.entering()
    }

    override fun visitNavDag(dag: ArbeidsgiverUtbetalingstidslinje.Utbetalingsdag.NavDag) {
        if (alderRegler.navBurdeBetale(betalteDager, 0, dag.dato)) {
            state.betalbarDag(this, dag.dato)
        } else {
            state.ikkeBetalbarDag(this, dag.dato)
        }
    }

    private sealed class State {
        open fun betalbarDag(
            avgrenser: Utbetalingsavgrenser,
            dagen: LocalDate
        ) {}
        open fun ikkeBetalbarDag(
            avgrenser: Utbetalingsavgrenser,
            dagen: LocalDate
        ) {}
        open fun entering() {}
        open fun leaving() {}

        internal object Initiell: State() {
            override fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.betalteDager = 1
                avgrenser.state(Syk)
            }
        }

        internal object Syk: State() {
            override fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.betalteDager += 1
            }

            override fun ikkeBetalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.ubetalteDager.add(dagen)
            }
        }

    }
}
