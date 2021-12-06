package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-12 ledd 1 punktum 1`
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-12 ledd 2`
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal data class Sykepengerettighet(
    val maksdato : LocalDate,
    val gjenståendeSykedager : Int,
    val forbrukteSykedager : Int
)

internal class MaksimumSykepengedagerfilter(
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val periode: Periode,
    private val aktivitetslogg: IAktivitetslogg
) : UtbetalingsdagVisitor {

    companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26 * 7
        private const val HISTORISK_PERIODE_I_ÅR: Long = 3
    }

    private lateinit var sisteUkedag: LocalDate
    private lateinit var sisteBetalteDag: LocalDate
    private lateinit var state: State
    private lateinit var teller: UtbetalingTeller
    private var opphold = 0
    private lateinit var sakensStartdato: LocalDate  // Date of first NAV payment in a new 248 period
    private lateinit var dekrementerfom: LocalDate  // Three year boundary from first sick day after a work day
    private val avvisteDatoerMedBegrunnelse = mutableMapOf<LocalDate, Begrunnelse>()
    private val avvisteDatoer get() = avvisteDatoerMedBegrunnelse.keys.toList()
    private val betalbarDager = mutableMapOf<LocalDate, NavDag>()
    private lateinit var beregnetTidslinje: Utbetalingstidslinje
    private lateinit var tidslinjegrunnlag: List<Utbetalingstidslinje>

    private fun maksdato() =
        if (gjenståendeSykedager() == 0) teller.maksdato(sisteBetalteDag) else teller.maksdato(sisteUkedag)

    private fun gjenståendeSykedager() = teller.gjenståendeSykepengedager(sisteBetalteDag)
    private fun forbrukteSykedager() = teller.forbrukteDager()

    internal fun filter(tidslinjer: List<Utbetalingstidslinje>, personTidslinje: Utbetalingstidslinje) : Sykepengerettighet {
        beregnetTidslinje = tidslinjer
            .reduce(Utbetalingstidslinje::plus)
            .plus(personTidslinje)
        tidslinjegrunnlag = tidslinjer + listOf(personTidslinje)
        teller = UtbetalingTeller(alder, arbeidsgiverRegler, aktivitetslogg)
        state = State.Initiell
        beregnetTidslinje.accept(this)
        if (::sakensStartdato.isInitialized) {
            val avvisteDager = avvisteDatoer.filter { sakensStartdato <= it }
            aktivitetslogg.`§8-12 ledd 1 punktum 1`(
                avvisteDager !in periode,
                avvisteDatoer.firstOrNull() ?: sakensStartdato,
                avvisteDatoer.lastOrNull() ?: sisteBetalteDag,
                tidslinjegrunnlag,
                beregnetTidslinje,
                gjenståendeSykedager(),
                forbrukteSykedager(),
                maksdato(),
                avvisteDager
            )
        }
        Utbetalingstidslinje.avvis(tidslinjer, avvisteDatoer.grupperSammenhengendePerioder(), periode) { dato: LocalDate ->
            avvisteDatoerMedBegrunnelse[dato]!!
        }

        if (avvisteDatoer in periode)
            aktivitetslogg.warn("Maks antall sykepengedager er nådd i perioden. Vurder å sende vedtaksbrev fra Infotrygd")
        else
            aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")

        return Sykepengerettighet(maksdato(), gjenståendeSykedager(), forbrukteSykedager())
    }

    private fun state(nyState: State) {
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        sisteUkedag = tidslinje.sisteUkedag()
        sisteBetalteDag = tidslinje.periode().endInclusive
    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        betalbarDager[dato] = dag
        state.betalbarDag(this, dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dag.dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dag.dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dag.dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dag.dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dag.dato)
    }

    override fun visit(
        dag: UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dag.dato)
    }

    private fun oppholdsdag(dagen: LocalDate) {
        opphold += 1
        state.oppholdsdag(this, dagen)
    }

    private fun nextState(dagen: LocalDate): State? {
        if (opphold >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) {
            aktivitetslogg.`§8-12 ledd 1 punktum 1`(
                avvisteDatoer.filter { sakensStartdato <= it } !in periode,
                avvisteDatoer.firstOrNull() ?: sakensStartdato,
                avvisteDatoer.lastOrNull() ?: sisteBetalteDag,
                tidslinjegrunnlag,
                beregnetTidslinje,
                gjenståendeSykedager(),
                forbrukteSykedager(),
                maksdato(),
                avvisteDatoer.filter { sakensStartdato <= it }
            )
            aktivitetslogg.`§8-12 ledd 2`(dagen, TILSTREKKELIG_OPPHOLD_I_SYKEDAGER, tidslinjegrunnlag, beregnetTidslinje)
            if (gjenståendeSykedager() <= 0 && dagen in periode) {
                aktivitetslogg.warn("26 uker siden forrige utbetaling av sykepenger, vurder om vilkårene for sykepenger er oppfylt")
            }
            teller.resett(dagen.plusDays(1))
            return State.Initiell
        }
        if (state == State.Karantene) return null
        return if (teller.påGrensen(dagen) {
                aktivitetslogg.`§8-12 ledd 1 punktum 1`(
                    true,
                    sakensStartdato,
                    sisteBetalteDag,
                    tidslinjegrunnlag,
                    beregnetTidslinje,
                    gjenståendeSykedager(),
                    forbrukteSykedager(),
                    maksdato(),
                    avvisteDatoer.filter { sakensStartdato <= it }
                )
            }) State.Karantene else null
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

        object Initiell : State() {
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

        object Syk : State() {
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

        object Opphold : State() {

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

        object Karantene : State() {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                avgrenser.avvisteDatoerMedBegrunnelse[dagen] = avgrenser.teller.begrunnelse
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }
        }

    }
}
