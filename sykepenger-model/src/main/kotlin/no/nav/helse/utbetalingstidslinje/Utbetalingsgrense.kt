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
        if (dag.inntekt == 0.0) return oppholdsdag(dag.dato)
        if (alderRegler.navBurdeBetale(betalteDager, gammelpersonDager, dag.dato)) {
            sisteBetalteDag = dag.dato
            state.betalbarDag(this, dag.dato)
        } else {
            state.ikkeBetalbarDag(this, dag.dato)
        }
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
        if (opphold < TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) {
            state.arbeidsdagIOppholdsperiode(this, dagen)
        } else {
            state(State.Initiell)
        }
    }

    private sealed class State {
        open fun betalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
        open fun ikkeBetalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
        open fun arbeidsdagIOppholdsperiode(avgrenser: Utbetalingsgrense, dagen: LocalDate) {}
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
                avgrenser.state(Karantene)
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
                avgrenser.opphold += 1
                avgrenser.state(Karantene)
            }
        }

        internal object Karantene: State() {
            override fun ikkeBetalbarDag(avgrenser: Utbetalingsgrense, dagen: LocalDate) {
                avgrenser.opphold += 1
                if (avgrenser.opphold >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return avgrenser.state(Initiell)
                avgrenser.ubetalteDager.add(Utbetalingstidslinje.Utbetalingsdag.AvvistDag(dagen, Begrunnelse.SykepengedagerOppbrukt))
            }
        }

    }
}
