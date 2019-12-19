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
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26*7
    }

    internal fun maksdato() = alderRegler.maksdato(betalteDager, gammelpersonDager, sisteBetalteDag)
    internal fun antallGjenståendeSykedager() = alderRegler.gjenståendeDager(betalteDager, gammelpersonDager, sisteBetalteDag)

    internal fun ubetalteDager(): List<Utbetalingstidslinje.Utbetalingsdag.AvvistDag> {
        return ubetalteDager
    }

    private fun state(nyState: State) {
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        state.betalbarDag(this, dag.dato)
    }

    override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
        oppholdsdag(dag.dato)
    }

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        oppholdsdag(dag.dato)
    }

    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        oppholdsdag(dag.dato)
    }

    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
        oppholdsdag(dag.dato)
    }

    private fun oppholdsdag(dagen: LocalDate) {
        opphold += 1
        state.oppholdsdag(this, dagen)
    }

    private fun nextState(dagen: LocalDate) : State? {
        if (opphold >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return State.Initiell
        return if (alderRegler.burdeBetale(betalteDager+1, gammelpersonDager+1, dagen.plusDays(1))) null else State.Karantene
    }

    private sealed class State {
        open fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
        open fun oppholdsdag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
        open fun entering(avgrenser: Utbetalingsgrense) {}
        open fun leaving(avgrenser: Utbetalingsgrense) {}

        internal object Initiell: State() {
            override fun entering(avgrenser: Utbetalingsgrense) {
                avgrenser.gammelpersonDager = 0
                avgrenser.betalteDager = 0
                avgrenser.opphold = 0
            }
            override fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.betalteDager = 1
                if (avgrenser.alderRegler.harFylt67(dagen) )  avgrenser.gammelpersonDager = 1
                avgrenser.sisteBetalteDag = dagen
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
                avgrenser.sisteBetalteDag = dagen
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }

            override fun oppholdsdag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.state(Opphold)
            }
        }

        internal object Opphold: State() {

            override fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.betalteDager += 1
                if (avgrenser.alderRegler.harFylt67(dagen) )  avgrenser.gammelpersonDager += 1
                avgrenser.sisteBetalteDag = dagen
                avgrenser.state(avgrenser.nextState(dagen) ?: Syk)
            }

            override fun oppholdsdag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }
        }

        internal object Karantene: State() {
            override fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.opphold += 1
                avgrenser.ubetalteDager.add(Utbetalingstidslinje.Utbetalingsdag.AvvistDag(dagen, Begrunnelse.SykepengedagerOppbrukt))
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }

            override fun oppholdsdag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }
        }

    }
}
