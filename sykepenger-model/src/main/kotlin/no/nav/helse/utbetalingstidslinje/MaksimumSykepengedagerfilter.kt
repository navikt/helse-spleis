package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Begrunnelse.SykepengedagerOppbrukt
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import java.time.LocalDate

internal class MaksimumSykepengedagerfilter(
    private val alder: Alder,
    arbeidsgiverRegler: ArbeidsgiverRegler,
    private val periode: Periode,
    private val aktivitetslogg: Aktivitetslogg
) : UtbetalingsdagVisitor {

    companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26 * 7
        private const val HISTORISK_PERIODE_I_ÅR: Long = 3
    }

    private lateinit var sisteUkedag: LocalDate
    private lateinit var sisteBetalteDag: LocalDate
    private var state: State = State.Initiell
    private val teller = UtbetalingTeller(alder, arbeidsgiverRegler)
    private var opphold = 0
    private lateinit var sakensStartdato: LocalDate  // Date of first NAV payment in a new 248 period
    private lateinit var dekrementerfom: LocalDate  // Three year boundary from first sick day after a work day
    private val avvisteDatoer = mutableListOf<LocalDate>()
    private val betalbarDager = mutableMapOf<LocalDate, NavDag>()

    internal fun maksdato() = if (gjenståendeSykedager() == 0) teller.maksdato(sisteBetalteDag) else teller.maksdato(sisteUkedag)
    internal fun gjenståendeSykedager() = teller.gjenståendeSykedager(sisteBetalteDag)
    internal fun forbrukteSykedager() = teller.forbrukteDager()

    internal fun filter(tidslinjer: List<Utbetalingstidslinje>, personTidslinje: Utbetalingstidslinje) {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere er ikke støttet enda" }
        val tidslinje = (tidslinjer + listOf(personTidslinje)).reduce(Utbetalingstidslinje::plus)
        tidslinje.accept(this)
        tidslinjer.forEach { it.avvis(avvisteDatoer, SykepengedagerOppbrukt) }

        if (avvisteDatoer in periode)
            aktivitetslogg.warn("Maks antall sykepengedager er nådd i perioden")
        else
            aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")
    }

    private fun state(nyState: State) {
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    override fun preVisit(tidslinje: Utbetalingstidslinje) {
        sisteUkedag = tidslinje.sisteUkedag()
        sisteBetalteDag = tidslinje.sisteDato()
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        if (dag.dato >= alder.øvreAldersgrense) state(State.Karantene) else betalbarDager[dag.dato] = dag
        state.betalbarDag(this, dag.dato)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
        oppholdsdag(dag.dato)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        oppholdsdag(dag.dato)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        oppholdsdag(dag.dato)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
        oppholdsdag(dag.dato)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
        oppholdsdag(dag.dato)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
        oppholdsdag(dag.dato)
    }

    private fun oppholdsdag(dagen: LocalDate) {
        opphold += 1
        state.oppholdsdag(this, dagen)
    }

    private fun nextState(dagen: LocalDate): State? {
        if (opphold >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) {
            teller.resett(dagen.plusDays(1))
            return State.Initiell
        }
        return if (teller.påGrensen(dagen)) State.Karantene else null
    }

    private fun dekrementer(tom: LocalDate) {
        val dekrementertom = tom.minusYears(HISTORISK_PERIODE_I_ÅR)
        if (dekrementertom >= sakensStartdato) {
            dekrementerfom.datesUntil(dekrementertom).forEach { dato ->
                betalbarDager[dato]?.also { teller.dekrementer(dato) }
            }
        }
        dekrementerfom = dekrementertom
    }

    private sealed class State {
        open fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        open fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        open fun entering(avgrenser: MaksimumSykepengedagerfilter) {}
        open fun leaving(avgrenser: MaksimumSykepengedagerfilter) {}

        internal object Initiell : State() {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.sakensStartdato = dagen
                avgrenser.dekrementerfom = dagen.minusYears(HISTORISK_PERIODE_I_ÅR)
                avgrenser.teller.inkrementer(dagen)
                avgrenser.sisteBetalteDag = dagen
                avgrenser.state(Syk)
            }
        }

        internal object Syk : State() {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller.inkrementer(dagen)
                avgrenser.sisteBetalteDag = dagen
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.state(Opphold)
            }
        }

        internal object Opphold : State() {

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller.inkrementer(dagen)
                avgrenser.sisteBetalteDag = dagen
                avgrenser.dekrementer(dagen)
                avgrenser.state(avgrenser.nextState(dagen) ?: Syk)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }
        }

        internal object Karantene : State() {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                avgrenser.avvisteDatoer.add(dagen)
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }
        }

    }
}
