package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_9
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.økonomi.Økonomi

internal class MaksimumSykepengedagerfilter(
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val infotrygdtidslinje: Utbetalingstidslinje
): UtbetalingstidslinjerFilter {

    private companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26 * 7
    }

    internal lateinit var maksimumSykepenger: Alder.MaksimumSykepenger
        private set

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonObserver: SubsumsjonObserver
    ): List<Utbetalingstidslinje> {
        val sisteDato = periode.endInclusive
        val beregnetTidslinje = tidslinjer
            .reduce(Utbetalingstidslinje::plus)
            .plus(infotrygdtidslinje).kutt(sisteDato)
        val tidslinjegrunnlag = tidslinjer + listOf(infotrygdtidslinje)

        val teller = ForbrukteSykepengedagerteller(alder, arbeidsgiverRegler, beregnetTidslinje, tidslinjegrunnlag, subsumsjonObserver)

        maksimumSykepenger = teller.maksimumSykepenger(aktivitetslogg, periode)
        return teller.avvisteTidslinjer(tidslinjer)
    }

    private class ForbrukteSykepengedagerteller(
        private val alder: Alder,
        private val arbeidsgiverRegler: ArbeidsgiverRegler,
        private val beregnetTidslinje: Utbetalingstidslinje,
        private val tidslinjegrunnlag: List<Utbetalingstidslinje>,
        private val subsumsjonObserver: SubsumsjonObserver
    ) : UtbetalingstidslinjeVisitor {
        private val teller = UtbetalingTeller(alder, arbeidsgiverRegler)
        private var state: State = State.Initiell
        private val sisteDag = beregnetTidslinje.periode().endInclusive
        private var opphold = 0
        private val begrunnelserForAvvisteDager = mutableMapOf<Begrunnelse, MutableSet<LocalDate>>()
        private val avvisteDager get() = begrunnelserForAvvisteDager.values.flatten().toSet()
        private val tidslinjegrunnlagsubsumsjon by lazy { tidslinjegrunnlag.subsumsjonsformat() }
        private val beregnetTidslinjesubsumsjon by lazy { beregnetTidslinje.subsumsjonsformat() }

        init {
            beregnetTidslinje.accept(this)
        }

        internal fun maksimumSykepenger(aktivitetslogg: IAktivitetslogg, periode: Periode) = teller.maksimumSykepenger(sisteDag).also {
            it.sisteDag(karantenesporing(periode, subsumsjonObserver))
            // TODO: logge juridisk vurdering for 70 år i "karantenesporing"
            alder.etterlevelse70år(aktivitetslogg, beregnetTidslinje.periode(), avvisteDager, subsumsjonObserver)

            if (begrunnelserForAvvisteDager[Begrunnelse.NyVilkårsprøvingNødvendig]?.any { it in periode } == true) {
                aktivitetslogg.funksjonellFeil(RV_VV_9)
            }
            if (avvisteDager in periode)
                aktivitetslogg.info("Maks antall sykepengedager er nådd i perioden")
            else
                aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")
        }

        internal fun avvisteTidslinjer(tidslinjer: List<Utbetalingstidslinje>) = begrunnelserForAvvisteDager.entries.fold(tidslinjer) { result, (begrunnelse, dager) ->
            Utbetalingstidslinje.avvis(result, dager.grupperSammenhengendePerioder(), listOf(begrunnelse))
        }

        private fun avvisDag(dag: LocalDate, begrunnelse: Begrunnelse) {
            begrunnelserForAvvisteDager.getOrPut(begrunnelse) {
                mutableSetOf()
            }.add(dag)
        }

        private fun karantenesporing(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = object : Alder.MaksimumSykepenger.Begrunnelse {
            override fun `§ 8-12 ledd 1 punktum 1`(sisteDag: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {
                val sakensStartdato = teller.startdatoSykepengerettighet() ?: return
                subsumsjonObserver.`§ 8-12 ledd 1 punktum 1`(
                    periode,
                    tidslinjegrunnlagsubsumsjon,
                    beregnetTidslinjesubsumsjon,
                    gjenståendeDager,
                    forbrukteDager,
                    sisteDag,
                    sakensStartdato
                )
            }

            override fun `§ 8-51 ledd 3`(sisteDag: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {
                val sakensStartdato = teller.startdatoSykepengerettighet() ?: return
                subsumsjonObserver.`§ 8-51 ledd 3`(
                    periode,
                    tidslinjegrunnlagsubsumsjon,
                    beregnetTidslinjesubsumsjon,
                    gjenståendeDager,
                    forbrukteDager,
                    sisteDag,
                    sakensStartdato
                )
            }

            override fun `§ 8-3 ledd 1 punktum 2`(sisteDag: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {}
        }

        private fun state(nyState: State) {
            if (this.state == nyState) return
            state.leaving(this)
            state = nyState
            state.entering(this)
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
            dag: Utbetalingsdag.NavHelgDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            if (alder.mistetSykepengerett(dato)) state(State.ForGammel)
            state.sykdomshelg(this, dato)
        }

        override fun visit(
            dag: Utbetalingsdag.Arbeidsdag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            oppholdsdag(dato)
        }

        override fun visit(
            dag: Utbetalingsdag.ArbeidsgiverperiodeDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            oppholdsdag(dato)
        }

        override fun visit(dag: Utbetalingsdag.ArbeidsgiverperiodedagNav, dato: LocalDate, økonomi: Økonomi) {
            oppholdsdag(dato)
        }

        override fun visit(
            dag: Utbetalingsdag.Fridag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            state.fridag(this, dato)
        }

        override fun visit(
            dag: Utbetalingsdag.AvvistDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            state.avvistDag(this, dato)
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
            val maksimumSykepenger = teller.maksimumSykepenger(dagen)
            (opphold >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).let { harTilstrekkeligOpphold ->
                subsumsjonObserver.`§ 8-12 ledd 2`(
                    oppfylt = harTilstrekkeligOpphold,
                    dato = dagen,
                    gjenståendeSykepengedager = maksimumSykepenger.gjenståendeDager(),
                    beregnetAntallOppholdsdager = opphold,
                    tilstrekkeligOppholdISykedager = TILSTREKKELIG_OPPHOLD_I_SYKEDAGER,
                    tidslinjegrunnlag = tidslinjegrunnlagsubsumsjon,
                    beregnetTidslinje = beregnetTidslinjesubsumsjon,
                )
                if (harTilstrekkeligOpphold) {
                    teller.resett()
                    return State.Initiell
                }
            }
            if (state == State.Karantene) return null
            if (maksimumSykepenger.sisteDag() > dagen) return null
            return State.Karantene
        }

        private interface State {
            fun betalbarDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {}
            fun avvistDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {}
            fun oppholdsdag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {}
            fun sykdomshelg(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {}
            fun fridag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) = avgrenser.oppholdsdag(dagen)
            fun entering(avgrenser: ForbrukteSykepengedagerteller) {}
            fun leaving(avgrenser: ForbrukteSykepengedagerteller) {}

            object Initiell : State {
                override fun entering(avgrenser: ForbrukteSykepengedagerteller) {
                    avgrenser.opphold = 0
                }

                override fun betalbarDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.teller.inkrementer(dagen)
                    avgrenser.teller.dekrementer(dagen)
                    avgrenser.state(Syk)
                }
            }

            object Syk : State {
                override fun entering(avgrenser: ForbrukteSykepengedagerteller) {
                    avgrenser.opphold = 0
                }

                override fun betalbarDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.teller.inkrementer(dagen)
                    avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
                    avgrenser.opphold = 0
                }

                override fun oppholdsdag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.state(avgrenser.nextState(dagen) ?: Opphold)
                }

                override fun fridag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.opphold += 1
                }
            }

            object Opphold : State {

                override fun betalbarDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.teller.inkrementer(dagen)
                    avgrenser.teller.dekrementer(dagen)
                    avgrenser.state(avgrenser.nextState(dagen) ?: Syk)
                }

                override fun sykdomshelg(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.opphold += 1
                    oppholdsdag(avgrenser, dagen)
                }

                override fun oppholdsdag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
                }
            }

            object Karantene : State {
                override fun betalbarDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.opphold += 1
                    avvistDag(avgrenser, dagen)
                }

                override fun avvistDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avvisDag(avgrenser, dagen)
                }

                private fun avvisDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.avvisDag(dagen, when (avgrenser.opphold > TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) {
                        true -> Begrunnelse.NyVilkårsprøvingNødvendig
                        false -> avgrenser.teller.maksimumSykepenger().begrunnelse()
                    })
                }

                override fun sykdomshelg(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.opphold += 1
                    /* helg skal ikke medføre ny rettighet */
                }

                override fun oppholdsdag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
                }

                override fun fridag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.opphold += 1
                }
            }

            object ForGammel : State {
                override fun betalbarDag(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    over70(avgrenser, dagen)
                }

                override fun sykdomshelg(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    over70(avgrenser, dagen)
                }

                override fun leaving(avgrenser: ForbrukteSykepengedagerteller) = throw IllegalStateException("Kan ikke gå ut fra state ForGammel")

                private fun over70(avgrenser: ForbrukteSykepengedagerteller, dagen: LocalDate) {
                    avgrenser.avvisDag(dagen, Begrunnelse.Over70)
                }
            }
        }
    }
}
