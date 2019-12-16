package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal class Utbetalingsgrense(private val alderRegler: AlderRegler):
    Utbetalingstidslinje.UtbetalingsdagVisitor {
    private var sisteBetalteDag: LocalDate? = null
    private var state: State = State.Initiell
    private var betalteDager = 0
    private var gammelpersonDager = 0
    private var opphold = 0
    private val ubetalteDager = mutableListOf<Utbetalingstidslinje.Utbetalingsdag.AvvistDag>()

    companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26*7-1
    }

    internal fun maksdato():LocalDate? = sisteBetalteDag?.let { alderRegler.maksdato(betalteDager, gammelpersonDager, it) }
    internal fun antallBetalteSykedager() = betalteDager

    internal fun ubetalteDager(): List<Utbetalingstidslinje.Utbetalingsdag.AvvistDag> {
        return ubetalteDager
    }

    private fun state(nyState: State) {
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        if (dag.inntekt == 0.0) return
        if (alderRegler.navBurdeBetale(betalteDager, gammelpersonDager, dag.dato)) {
            sisteBetalteDag = dag.dato
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
        open fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
        open fun ikkeBetalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
        open fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
        open fun arbeidsdagEtterOppholdsperiode(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
        open fun entering(avgrenser: Utbetalingsgrense) {}
        open fun leaving(avgrenser: Utbetalingsgrense) {}

        internal object Initiell: State() {
            override fun entering(avgrenser: Utbetalingsgrense) {
                avgrenser.gammelpersonDager = 0
                avgrenser.betalteDager = 0
            }
            override fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.betalteDager = 1
                if (avgrenser.alderRegler.harFylt67(dagen) )  avgrenser.gammelpersonDager = 1
                avgrenser.state(Syk)
            }
        }

        internal object Syk: State() {
            override fun entering(avgrenser: Utbetalingsgrense) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.betalteDager += 1
                if (avgrenser.alderRegler.harFylt67(dagen) )  avgrenser.gammelpersonDager += 1
            }

            override fun ikkeBetalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.ubetalteDager.add(Utbetalingstidslinje.Utbetalingsdag.AvvistDag(dagen, Begrunnelse.SykepengedagerOppbrukt))
            }

            override fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.opphold = 1
                avgrenser.state(Opphold)
            }
        }

        internal object Opphold: State() {

            override fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.betalteDager += 1
                if (avgrenser.alderRegler.harFylt67(dagen) )  avgrenser.gammelpersonDager += 1
                avgrenser.state(Syk)
            }

            override fun ikkeBetalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.ubetalteDager.add(Utbetalingstidslinje.Utbetalingsdag.AvvistDag(dagen, Begrunnelse.SykepengedagerOppbrukt))
                avgrenser.state(Syk)
            }

            override fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.opphold += 1
            }

            override fun arbeidsdagEtterOppholdsperiode(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.opphold = 0
                avgrenser.state(Initiell)
            }

        }

    }
}
