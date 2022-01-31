package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.toSubsumsjonFormat
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class MaksimumSykepengedagerfilter(
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val periode: Periode,
    private val aktivitetslogg: IAktivitetslogg,
    private val subsumsjonObserver: SubsumsjonObserver
) : UtbetalingsdagVisitor {

    private companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26 * 7
    }

    private lateinit var sisteDag: LocalDate
    private lateinit var sisteBetalteDag: LocalDate
    private lateinit var state: State
    private lateinit var teller: UtbetalingTeller
    private var opphold = 0
    private lateinit var sakensStartdato: LocalDate  // Date of first NAV payment in a new 248 period
    private val begrunnelserForAvvisteDager = mutableMapOf<Begrunnelse, MutableSet<LocalDate>>()
    private val avvisteDager get() = begrunnelserForAvvisteDager.values.flatten().toSet()
    private lateinit var beregnetTidslinje: Utbetalingstidslinje
    private lateinit var tidslinjegrunnlag: List<Utbetalingstidslinje>

    private fun avvisDag(dag: LocalDate, begrunnelse: Begrunnelse) {
        begrunnelserForAvvisteDager.getOrPut(begrunnelse) {
            mutableSetOf()
        }.add(dag)
    }

    private val maksimumSykepengersporing = object : Alder.MaksimumSykepenger.Begrunnelse {
        override fun `§ 8-12 ledd 1 punktum 1`(dato: LocalDate, gjenståendeSykepengedager: Int) {
            // TODO: venter på avklaring om vi skal lagre vurdering for beregninger
        }

        override fun `§ 8-51 ledd 3`(dato: LocalDate, gjenståendeSykepengedager: Int) {
            // TODO: venter på avklaring om vi skal lagre vurdering for beregninger
            /*aktivitetslogg.`§8-51 ledd 3`(
                oppfylt = gjenståendeSykepengedager > 0,
                maksSykepengedagerOver67 = arbeidsgiverRegler.maksSykepengedagerOver67(),
                gjenståendeSykedager = gjenståendeSykepengedager,
                forbrukteSykedager = forbrukteDager,
                maksdato = dato
            )*/
        }

        override fun `§ 8-3 ledd 1 punktum 2`(dato: LocalDate, gjenståendeSykepengedager: Int) {
            // TODO: venter på avklaring om vi skal lagre vurdering for beregninger
        }
    }

    internal fun filter(tidslinjer: List<Utbetalingstidslinje>, personTidslinje: Utbetalingstidslinje): Alder.MaksimumSykepenger {
        beregnetTidslinje = tidslinjer
            .reduce(Utbetalingstidslinje::plus)
            .plus(personTidslinje)
        tidslinjegrunnlag = tidslinjer + listOf(personTidslinje)
        teller = UtbetalingTeller(alder, arbeidsgiverRegler, subsumsjonObserver)
        state = State.Initiell
        beregnetTidslinje.accept(this)

        val maksimumSykepenger = teller.maksimumSykepenger(sisteBetalteDag, sisteDag)
        val maksdato = maksimumSykepenger.sisteDag(maksimumSykepengersporing)
        val gjenståendeSykedager = maksimumSykepenger.gjenståendeDager(maksimumSykepengersporing)
        val forbrukteSykedager = maksimumSykepenger.forbrukteDager()

        if (::sakensStartdato.isInitialized) {
            val avvisteDager = avvisteDager.filter { sakensStartdato <= it }
            subsumsjonObserver.`§ 8-12 ledd 1 punktum 1`(
                avvisteDager !in periode,
                periode,
                avvisteDager.firstOrNull() ?: sakensStartdato,
                avvisteDager.lastOrNull() ?: sisteBetalteDag,
                tidslinjegrunnlag.toSubsumsjonFormat(),
                beregnetTidslinje.toSubsumsjonFormat(),
                gjenståendeSykedager,
                forbrukteSykedager,
                maksdato,
                avvisteDager
            )
        }

        begrunnelserForAvvisteDager.forEach { (begrunnelse, avvisteDager) ->
            Utbetalingstidslinje.avvis(
                tidslinjer = tidslinjer,
                avvistePerioder = avvisteDager.grupperSammenhengendePerioder(),
                begrunnelser = listOf(begrunnelse)
            )
        }

        if (avvisteDager in periode)
            aktivitetslogg.warn("Maks antall sykepengedager er nådd i perioden. Vurder å sende vedtaksbrev fra Infotrygd")
        else
            aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")

        return maksimumSykepenger
    }

    private fun state(nyState: State) {
        if (this.state == nyState) return
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        sisteDag = tidslinje.periode().endInclusive
        sisteBetalteDag = sisteDag // TODO: fjerne sisteBetalteDag
    }

    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        alder.etterlevelse70år(aktivitetslogg, beregnetTidslinje.periode(), avvisteDager, subsumsjonObserver)
    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (alder.mistetSykepengerett(dato)) state(State.ForGammel)
        state.betalbarDag(this, dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (alder.mistetSykepengerett(dato)) state(State.ForGammel)
        state.sykdomshelg(this, dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        state.fridag(this, dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
    }

    override fun visit(
        dag: UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
    }

    private fun oppholdsdag(dagen: LocalDate) {
        opphold += 1
        state.oppholdsdag(this, dagen)
    }

    private fun nextState(dagen: LocalDate): State? {
        (opphold >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).let { harTilstrekkeligOpphold ->
            subsumsjonObserver.`§ 8-12 ledd 2`(
                oppfylt = harTilstrekkeligOpphold,
                dato = dagen,
                gjenståendeSykepengedager = teller.maksimumSykepenger(sisteBetalteDag).gjenståendeDager(),
                beregnetAntallOppholdsdager = opphold,
                tilstrekkeligOppholdISykedager = TILSTREKKELIG_OPPHOLD_I_SYKEDAGER,
                tidslinjegrunnlag = tidslinjegrunnlag.toSubsumsjonFormat(),
                beregnetTidslinje = beregnetTidslinje.toSubsumsjonFormat(),
            )
            if (harTilstrekkeligOpphold) {
                teller.resett()
                return State.Initiell
            }
        }
        if (state == State.Karantene) return null
        if (teller.erFørMaksdato(dagen)) return null

        return State.Karantene
    }

    private interface State {
        fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) = avgrenser.oppholdsdag(dagen)
        fun entering(avgrenser: MaksimumSykepengedagerfilter) {}
        fun leaving(avgrenser: MaksimumSykepengedagerfilter) {}

        object Initiell : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.sakensStartdato = dagen
                avgrenser.teller.inkrementer(dagen)
                avgrenser.sisteBetalteDag = dagen
                avgrenser.teller.dekrementer(dagen)
                avgrenser.state(Syk)
            }
        }

        object Syk : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller.inkrementer(dagen)
                avgrenser.sisteBetalteDag = dagen
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
                avgrenser.opphold = 0
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.state(avgrenser.nextState(dagen) ?: Opphold)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
            }
        }

        object Opphold : State {

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller.inkrementer(dagen)
                avgrenser.sisteBetalteDag = dagen
                avgrenser.teller.dekrementer(dagen)
                avgrenser.state(avgrenser.nextState(dagen) ?: Syk)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                oppholdsdag(avgrenser, dagen)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }
        }

        object Karantene : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                val maksimumSykepenger = avgrenser.teller.maksimumSykepenger(avgrenser.sisteBetalteDag)
                val maksdato = maksimumSykepenger.sisteDag(avgrenser.maksimumSykepengersporing)
                val gjenståendeSykedager = maksimumSykepenger.gjenståendeDager(avgrenser.maksimumSykepengersporing)
                val forbrukteSykedager = maksimumSykepenger.forbrukteDager()
                avgrenser.teller.hvisGrensenErNådd(
                    hvis248Dager = {
                        avgrenser.subsumsjonObserver.`§ 8-12 ledd 1 punktum 1`(
                            true,
                            avgrenser.periode,
                            avgrenser.sakensStartdato,
                            avgrenser.sisteBetalteDag,
                            avgrenser.tidslinjegrunnlag.toSubsumsjonFormat(),
                            avgrenser.beregnetTidslinje.toSubsumsjonFormat(),
                            gjenståendeSykedager,
                            forbrukteSykedager,
                            maksdato,
                            avgrenser.avvisteDager.filter { avgrenser.sakensStartdato <= it }
                        )
                    }
                )
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                avgrenser.avvisDag(dagen, when (avgrenser.opphold > TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) {
                    true -> Begrunnelse.NyVilkårsprøvingNødvendig
                    false -> avgrenser.teller.begrunnelse(dagen)
                })
                avgrenser.teller.hvisGrensenErNådd(
                    hvis67År = { maksdato ->
                        avgrenser.teller.`§8-51 ledd 3`(maksdato)
                    }
                )
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                /* helg skal ikke medføre ny rettighet */
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
            }
        }

        object ForGammel : State {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                over70(avgrenser, dagen)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                over70(avgrenser, dagen)
            }

            override fun leaving(avgrenser: MaksimumSykepengedagerfilter) = throw IllegalStateException("Kan ikke gå ut fra state ForGammel")

            private fun over70(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.avvisDag(dagen, Begrunnelse.Over70)
            }
        }
    }
}
