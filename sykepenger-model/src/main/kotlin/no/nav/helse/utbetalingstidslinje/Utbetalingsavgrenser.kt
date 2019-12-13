package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal class Utbetalingsavgrenser(private val tidslinje: Utbetalingstidslinje,
                                    private val alderRegler: AlderRegler):
    Utbetalingstidslinje.UtbetalingsdagVisitor {
    private var state: State = State.Initiell
    private var betalteDager = 0
    private var gammelmannDager = 0
    private var opphold = 0
    private val ubetalteDager = mutableListOf<Utbetalingstidslinje.Utbetalingsdag.AvvistDag>()

    companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26*7-1
    }

    internal fun ubetalteDager(): List<Utbetalingstidslinje.Utbetalingsdag.AvvistDag> {
        tidslinje.accept(this)
        return ubetalteDager
    }

    private fun state(nyState: State) {
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        if (dag.inntekt == 0.0) return
        if (alderRegler.navBurdeBetale(betalteDager, gammelmannDager, dag.dato)) {
            state.betalbarDag(this, dag.dato)
        } else {
            state.ikkeBetalbarDag(this, dag.dato)
        }
    }

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        arbeidsdag(dag.dato)
    }

    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        arbeidsdag(dag.dato)
    }

    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
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
        open fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {}
        open fun ikkeBetalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {}
        open fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {}
        open fun arbeidsdagEtterOppholdsperiode(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {}
        open fun entering(avgrenser: Utbetalingsavgrenser) {}
        open fun leaving(avgrenser: Utbetalingsavgrenser) {}

        internal object Initiell: State() {
            override fun entering(avgrenser: Utbetalingsavgrenser) {
                avgrenser.gammelmannDager = 0
                avgrenser.betalteDager = 0
            }
            override fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.betalteDager = 1
                if (avgrenser.alderRegler.harFylt67(dagen) )  avgrenser.gammelmannDager = 1
                avgrenser.state(Syk)
            }
        }

        internal object Syk: State() {
            override fun entering(avgrenser: Utbetalingsavgrenser) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.betalteDager += 1
                if (avgrenser.alderRegler.harFylt67(dagen) )  avgrenser.gammelmannDager += 1
            }

            override fun ikkeBetalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.ubetalteDager.add(Utbetalingstidslinje.Utbetalingsdag.AvvistDag(dagen, Begrunnelse.SykepengedagerOppbrukt))
            }

            override fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.opphold = 1
                avgrenser.state(Opphold)
            }
        }

        internal object Opphold: State() {

            override fun betalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.betalteDager += 1
                if (avgrenser.alderRegler.harFylt67(dagen) )  avgrenser.gammelmannDager += 1
                avgrenser.state(Syk)
            }

            override fun ikkeBetalbarDag(avgrenser: Utbetalingsavgrenser, dagen: LocalDate) {
                avgrenser.ubetalteDager.add(Utbetalingstidslinje.Utbetalingsdag.AvvistDag(dagen, Begrunnelse.SykepengedagerOppbrukt))
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
