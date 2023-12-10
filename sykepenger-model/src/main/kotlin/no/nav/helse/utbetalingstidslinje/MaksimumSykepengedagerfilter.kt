package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.etterlevelse.SubsumsjonObserver.Companion.NullObserver
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
): UtbetalingstidslinjerFilter, UtbetalingstidslinjeVisitor {

    private companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26 * 7
        private const val HISTORISK_PERIODE_I_ÅR: Long = 3
    }

    private var teller: Maksdatosituasjon? = null
    private var state: State = State.Initiell
    private var opphold = 0
    private val begrunnelserForAvvisteDager = mutableMapOf<Begrunnelse, MutableSet<LocalDate>>()
    private val avvisteDager get() = begrunnelserForAvvisteDager.values.flatten().toSet()
    private lateinit var beregnetTidslinje: Utbetalingstidslinje
    private lateinit var tidslinjegrunnlag: List<Utbetalingstidslinje>
    private var subsumsjonObserver: SubsumsjonObserver = NullObserver

    private val tidslinjegrunnlagsubsumsjon by lazy { tidslinjegrunnlag.subsumsjonsformat() }
    private val beregnetTidslinjesubsumsjon by lazy { beregnetTidslinje.subsumsjonsformat() }

    private val maksdatoer = mutableMapOf<LocalDate, Maksdatosituasjon>()

    internal fun maksimumSykepenger(periode: Periode, subsumsjonObserver: SubsumsjonObserver): Maksdatosituasjon {
        val maksimumSykepenger = maksdatoer.getValue(periode.endInclusive)
        maksimumSykepenger.vurderMaksdatobestemmelse(subsumsjonObserver, periode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, avvisteDager)
        return maksimumSykepenger
    }

    private fun avvisDag(dag: LocalDate, begrunnelse: Begrunnelse) {
        begrunnelserForAvvisteDager.getOrPut(begrunnelse) {
            mutableSetOf()
        }.add(dag)
    }

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonObserver: SubsumsjonObserver
    ): List<Utbetalingstidslinje> {
        this.subsumsjonObserver = subsumsjonObserver
        tidslinjegrunnlag = tidslinjer + listOf(infotrygdtidslinje)
        beregnetTidslinje = tidslinjegrunnlag.reduce(Utbetalingstidslinje::plus)
        beregnetTidslinje.accept(this)

        val avvisteTidslinjer = begrunnelserForAvvisteDager.entries.fold(tidslinjer) { result, (begrunnelse, dager) ->
            Utbetalingstidslinje.avvis(result, dager.grupperSammenhengendePerioder(), listOf(begrunnelse))
        }

        if (begrunnelserForAvvisteDager[Begrunnelse.NyVilkårsprøvingNødvendig]?.any { it in periode } == true) {
            aktivitetslogg.funksjonellFeil(RV_VV_9)
        }
        if (avvisteDager in periode)
            aktivitetslogg.info("Maks antall sykepengedager er nådd i perioden")
        else
            aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")

        return avvisteTidslinjer
    }

    private fun state(nyState: State) {
        if (this.state == nyState) return
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    private fun Maksdatosituasjon?.maksdatoFor(dato: LocalDate): Maksdatosituasjon {
        return this?.maksdatoFor(dato) ?: Maksdatosituasjon(arbeidsgiverRegler, dato, alder, LocalDate.MIN, LocalDate.MIN, emptySet())
    }

    override fun visit(dag: Utbetalingsdag.ForeldetDag, dato: LocalDate, økonomi: Økonomi) {
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (alder.mistetSykepengerett(dato)) state(State.ForGammel)
        state.betalbarDag(this, dato)
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (alder.mistetSykepengerett(dato)) state(State.ForGammel)
        state.sykdomshelg(this, dato)
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    override fun visit(dag: Utbetalingsdag.ArbeidsgiverperiodedagNav, dato: LocalDate, økonomi: Økonomi) {
        oppholdsdag(dato)
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        fridag(dato)
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    override fun visit(
        dag: Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        state.avvistDag(this, dato)
        oppholdsdag(dato)
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    override fun visit(
        dag: UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
        maksdatoer[dato] = this.teller.maksdatoFor(dato)
    }

    private fun oppholdsdag(dagen: LocalDate) {
        opphold += 1
        state.oppholdsdag(this, dagen)
    }

    private fun fridag(dagen: LocalDate) {
        opphold += 1
        state.fridag(this, dagen)
    }

    private fun håndterBetalbarDag(dagen: LocalDate) {
        var situasjon = checkNotNull(this.teller)
        situasjon = situasjon.inkrementer(dagen)
        situasjon.vurderHarTilstrekkeligOpphold(subsumsjonObserver, opphold, TILSTREKKELIG_OPPHOLD_I_SYKEDAGER, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon)
        when {
            situasjon.erDagerUnder67ÅrForbrukte() -> state(State.Karantene(Begrunnelse.SykepengedagerOppbrukt))
            situasjon.erDagerOver67ÅrForbrukte() -> state(State.Karantene(Begrunnelse.SykepengedagerOppbruktOver67))
            else -> state(State.Syk)
        }
        this.teller = situasjon
    }
    private fun tilstrekkeligOppholdNådd(dagen: LocalDate): Boolean {
        return checkNotNull(this.teller).maksdatoFor(dagen).vurderHarTilstrekkeligOpphold(subsumsjonObserver, opphold, TILSTREKKELIG_OPPHOLD_I_SYKEDAGER, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon)
    }

    private interface State {
        fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun avvistDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) = oppholdsdag(avgrenser, dagen)
        fun entering(avgrenser: MaksimumSykepengedagerfilter) {}
        fun leaving(avgrenser: MaksimumSykepengedagerfilter) {}

        object Initiell : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.teller = null
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller = Maksdatosituasjon(avgrenser.arbeidsgiverRegler, dagen, avgrenser.alder, dagen, dagen.minusYears(HISTORISK_PERIODE_I_ÅR), setOf(dagen))
                avgrenser.state(Syk)
            }
        }

        object Syk : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.håndterBetalbarDag(dagen)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.state(Opphold)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.state(OppholdFri)
            }
        }

        object Opphold : State {

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller = checkNotNull(avgrenser.teller).dekrementer(dagen.minusYears(HISTORISK_PERIODE_I_ÅR))
                avgrenser.håndterBetalbarDag(dagen)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                oppholdsdag(avgrenser, dagen)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                if (!avgrenser.tilstrekkeligOppholdNådd(dagen)) return
                avgrenser.state(Initiell)
            }
        }

        object OppholdFri : State {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.håndterBetalbarDag(dagen)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                if (!avgrenser.tilstrekkeligOppholdNådd(dagen)) return
                avgrenser.state(Initiell)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                if (!avgrenser.tilstrekkeligOppholdNådd(dagen)) return avgrenser.state(Opphold)
                avgrenser.state(Initiell)
            }
        }

        class Karantene(private val begrunnelse: Begrunnelse) : State {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                vurderTilstrekkeligOppholdNådd(avgrenser)
                avgrenser.state.avvistDag(avgrenser, dagen)
            }

            override fun avvistDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.avvisDag(dagen, begrunnelse)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                /* helg skal ikke medføre ny rettighet */
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                if (!avgrenser.tilstrekkeligOppholdNådd(dagen)) return
                avgrenser.state(Initiell)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            private fun vurderTilstrekkeligOppholdNådd(avgrenser: MaksimumSykepengedagerfilter) {
                if (avgrenser.opphold <= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return
                avgrenser.state(KaranteneTilstrekkeligOppholdNådd)
            }
        }

        object KaranteneTilstrekkeligOppholdNådd : State {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avvistDag(avgrenser, dagen)
            }

            override fun avvistDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.avvisDag(dagen, Begrunnelse.NyVilkårsprøvingNødvendig)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.state(Initiell)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
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
