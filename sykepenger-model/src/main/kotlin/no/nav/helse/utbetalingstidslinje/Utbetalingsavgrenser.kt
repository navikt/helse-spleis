package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal class Utbetalingsavgrenser(private val tidslinje: ArbeidsgiverUtbetalingstidslinje,
                                    private val alderRegler: AlderRegler):
    ArbeidsgiverUtbetalingstidslinje.UtbetalingsdagVisitor() {
    private var state: State = State.Initiell
    private var betalteDager = 0
    private var opphold = 0
    private val ubetalteDager = mutableListOf<LocalDate>()

    companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26*7-1
    }

    internal fun ubetalteDager(): List<LocalDate> {
        tidslinje.accept(this)
        return ubetalteDager
    }

    private fun state(nyState: State) {
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    override fun visitNavDag(dag: ArbeidsgiverUtbetalingstidslinje.Utbetalingsdag.NavDag) {
        if (alderRegler.navBurdeBetale(betalteDager, 0, dag.dato)) {
            state.betalbarDag(this, dag.dato)
        } else {
            state.ikkeBetalbarDag(this, dag.dato)
        }
    }

    override fun visitArbeidsdag(dag: ArbeidsgiverUtbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        arbeidsdag(dag.dato)
    }

    override fun visitArbeidsgiverperiodeDag(dag: ArbeidsgiverUtbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        arbeidsdag(dag.dato)
    }

    override fun visitFridag(dag: ArbeidsgiverUtbetalingstidslinje.Utbetalingsdag.Fridag) {
        arbeidsdag(dag.dato)
    }

    private fun arbeidsdag(dagen: LocalDate) {
        if (opphold < TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) {
            state.arbeidsdagIOppholdsperiode(this, dagen)
        } else {
            state.arbeidsdagEtterOppholdsperiode(this, dagen)
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
        open fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {}
        open fun arbeidsdagEtterOppholdsperiode(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {}
        open fun entering(avgrenser: Utbetalingsavgrenser) {}
        open fun leaving(avgrenser: Utbetalingsavgrenser) {}

        internal object Initiell: State() {
            override fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.betalteDager = 1
                avgrenser.state(Syk)
            }
        }

        internal object Syk: State() {
            override fun entering(avgrenser: Utbetalingsavgrenser) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.betalteDager += 1
            }

            override fun ikkeBetalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.ubetalteDager.add(dagen)
            }

            override fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.opphold = 1
                avgrenser.state(Opphold)
            }
        }

        internal object Opphold: State() {

            override fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.betalteDager += 1
                avgrenser.state(Syk)
            }

            override fun ikkeBetalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.ubetalteDager.add(dagen)
                avgrenser.state(Syk)
            }

            override fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.opphold += 1
            }

            override fun arbeidsdagEtterOppholdsperiode(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.opphold = 0
                avgrenser.state(Initiell)
            }

        }

    }
}
