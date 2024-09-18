package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.NullObserver
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_9
import no.nav.helse.plus
import no.nav.helse.ukedager
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter.State.Karantene
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter.State.Syk
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

    private val tidligereVurderinger = mutableMapOf<LocalDate, Maksdatokontekst>()
    private var sisteVurdering = Maksdatokontekst.TomKontekst
        set(value) {
            /* lagrer gammel verdi for å kunne plukke den opp senere, ifm. maksdatovurderinger på tvers av arbeidsgivere med ulike tom */
            tidligereVurderinger.putIfAbsent(field.vurdertTilOgMed, field)
            field = value
        }

    private var state: State = State.Initiell
    private val begrunnelserForAvvisteDager = mutableMapOf<Begrunnelse, MutableSet<LocalDate>>()
    private val avvisteDager get() = begrunnelserForAvvisteDager.values.flatten().toSet()
    private lateinit var beregnetTidslinje: Utbetalingstidslinje
    private lateinit var tidslinjegrunnlag: List<Utbetalingstidslinje>
    private var subsumsjonslogg: Subsumsjonslogg = NullObserver

    private val tidslinjegrunnlagsubsumsjon by lazy { tidslinjegrunnlag.subsumsjonsformat() }
    private val beregnetTidslinjesubsumsjon by lazy { beregnetTidslinje.subsumsjonsformat() }

    internal fun maksdatoresultatForVedtaksperiode(periode: Periode, subsumsjonslogg: Subsumsjonslogg): Maksdatoresultat {
        val sisteVurderingForut = tidligereVurderinger.values
            .sortedBy { it.vurdertTilOgMed }
            .lastOrNull { it.vurdertTilOgMed < periode.endInclusive }

        /* pga. mursteins-problematikk så kan vi ikke anta at siste vurdering som er gjort
            vil være aktuelt for alle vedtaksperiodene. en vedtaksperiode kan strekke seg lengre enn
            en annen den overlapper med, og siste vurdering vil være knyttet opp til den siste dagen på den samlede
            utbetalingstidslinjen. Hvis alle overlappende vedtaksperioder sluttet samme dag ville dette ikke vært et problem.
            det blir også kun lagret en ny vurdering for dager som faktisk betyr noe, derfor er det ikke nødvendigvis
            vurdering for hver eneste dag. om vi ikke finner en vurdering så vil forrige vurdering forut være riktig (siden
             det skal verken ha vært opphold eller utbetaling i mellomtiden).
         */
        val riktigKontekst = when {
            // vi har ikke gjort en konkret vurdering, så da strekker vi siste vurdering frem
            sisteVurdering.vurdertTilOgMed < periode.endInclusive -> sisteVurdering.copy(vurdertTilOgMed = periode.endInclusive)
            // konkret vurdering for dagen er gjort
            sisteVurdering.vurdertTilOgMed == periode.endInclusive -> sisteVurdering
            // konkret vurdering for dagen er gjort
            periode.endInclusive in tidligereVurderinger -> tidligereVurderinger.getValue(periode.endInclusive)
            // tar utgangspunkt i siste vurdering før dato, og strekker den frem
            sisteVurderingForut != null -> sisteVurderingForut.copy(vurdertTilOgMed = periode.endInclusive)
            else -> error("Finner ikke vurdering for ${periode.endInclusive}")
        }

        return beregnMaksdatoOgSubsummer(periode, subsumsjonslogg, riktigKontekst, beregnetTidslinje)
    }

    private fun beregnMaksdatoOgSubsummer(vedtaksperiode: Periode, subsumsjonslogg: Subsumsjonslogg, maksdatokontekst: Maksdatokontekst, samletGrunnlagstidslinje: Utbetalingstidslinje): Maksdatoresultat {
        fun LocalDate.forrigeVirkedagFør() = minusDays(when (dayOfWeek) {
            SUNDAY -> 2
            MONDAY -> 3
            else -> 1
        })
        fun LocalDate.sisteVirkedagInklusiv() = when (dayOfWeek) {
            SATURDAY -> minusDays(1)
            SUNDAY -> minusDays(2)
            else -> this
        }

        val førSyttiårsdagen = fun (subsumsjonslogg: Subsumsjonslogg, utfallTom: LocalDate) {
            subsumsjonslogg.`§ 8-3 ledd 1 punktum 2`(
                oppfylt = true,
                syttiårsdagen = alder.syttiårsdagen,
                utfallFom = vedtaksperiode.start,
                utfallTom = utfallTom,
                tidslinjeFom = vedtaksperiode.start,
                tidslinjeTom = vedtaksperiode.endInclusive,
                avvistePerioder = emptyList()
            )
        }

        val harNåddMaks = maksdatokontekst.erDagerOver67ÅrForbrukte(alder, arbeidsgiverRegler) || maksdatokontekst.erDagerUnder67ÅrForbrukte(alder, arbeidsgiverRegler)
        val forrigeMaksdato = if (harNåddMaks) maksdatokontekst.betalteDager.last() else null
        val forrigeVirkedag = forrigeMaksdato ?: maksdatokontekst.vurdertTilOgMed.sisteVirkedagInklusiv()

        val maksdatoOrdinærRett = forrigeVirkedag + maksdatokontekst.gjenståendeDagerUnder67År(alder, arbeidsgiverRegler).ukedager
        val maksdatoBegrensetRett = maxOf(forrigeVirkedag, alder.redusertYtelseAlder.sisteVirkedagInklusiv()) + maksdatokontekst.gjenståendeDagerOver67År(alder, arbeidsgiverRegler).ukedager

        val hjemmelsbegrunnelse: Maksdatoresultat.Bestemmelse
        val maksdato: LocalDate
        val gjenståendeDager: Int
        // maksdato er den dagen som først inntreffer blant ordinær kvote, 67-års-kvoten og 70-årsdagen,
        // med mindre man allerede har brukt opp alt tidligere
        when {
            maksdatoOrdinærRett <= maksdatoBegrensetRett -> {
                maksdato = maksdatoOrdinærRett
                gjenståendeDager = maksdatokontekst.gjenståendeDagerUnder67År(alder, arbeidsgiverRegler)
                hjemmelsbegrunnelse = Maksdatoresultat.Bestemmelse.ORDINÆR_RETT

                subsumsjonslogg.`§ 8-12 ledd 1 punktum 1`(vedtaksperiode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, gjenståendeDager, maksdatokontekst.betalteDager.size, maksdato, maksdatokontekst.startdatoSykepengerettighet)
                førSyttiårsdagen(subsumsjonslogg, vedtaksperiode.endInclusive)
            }
            maksdatoBegrensetRett <= alder.syttiårsdagen.forrigeVirkedagFør() -> {
                maksdato = maksdatoBegrensetRett
                gjenståendeDager = ukedager(forrigeVirkedag, maksdato)
                hjemmelsbegrunnelse = Maksdatoresultat.Bestemmelse.BEGRENSET_RETT

                subsumsjonslogg.`§ 8-51 ledd 3`(vedtaksperiode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, gjenståendeDager, maksdatokontekst.betalteDager.size, maksdato, maksdatokontekst.startdatoSykepengerettighet)
                førSyttiårsdagen(subsumsjonslogg, alder.syttiårsdagen.forrigeDag)
            }
            else -> {
                maksdato = alder.syttiårsdagen.forrigeVirkedagFør()
                gjenståendeDager = ukedager(forrigeVirkedag, maksdato)
                hjemmelsbegrunnelse = Maksdatoresultat.Bestemmelse.SYTTI_ÅR

                if (vedtaksperiode.start < alder.syttiårsdagen) {
                    førSyttiårsdagen(subsumsjonslogg, alder.syttiårsdagen.forrigeDag)
                }

                val avvisteDagerFraOgMedSøtti = avvisteDager.filter { it >= alder.syttiårsdagen }
                if (avvisteDagerFraOgMedSøtti.isNotEmpty()) {
                    subsumsjonslogg.`§ 8-3 ledd 1 punktum 2`(
                        oppfylt = false,
                        syttiårsdagen = alder.syttiårsdagen,
                        utfallFom = maxOf(alder.syttiårsdagen, vedtaksperiode.start),
                        utfallTom = vedtaksperiode.endInclusive,
                        tidslinjeFom = vedtaksperiode.start,
                        tidslinjeTom = vedtaksperiode.endInclusive,
                        avvistePerioder = avvisteDagerFraOgMedSøtti.grupperSammenhengendePerioder()
                    )
                }
            }
        }

        val tidligsteDag = if (maksdatokontekst.startdatoSykepengerettighet == LocalDate.MIN) maksdatokontekst.startdatoTreårsvindu else minOf(maksdatokontekst.startdatoTreårsvindu, maksdatokontekst.startdatoSykepengerettighet)
        return Maksdatoresultat(
            vurdertTilOgMed = maksdatokontekst.vurdertTilOgMed,
            bestemmelse = hjemmelsbegrunnelse,
            startdatoTreårsvindu = maksdatokontekst.startdatoTreårsvindu,
            startdatoSykepengerettighet = maksdatokontekst.startdatoSykepengerettighet.takeUnless { it == LocalDate.MIN },
            forbrukteDager = maksdatokontekst.betalteDager,
            maksdato = maksdato,
            gjenståendeDager = gjenståendeDager,
            grunnlag = samletGrunnlagstidslinje.subset(tidligsteDag til maksdatokontekst.vurdertTilOgMed)
        )
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
        subsumsjonslogg: Subsumsjonslogg
    ): List<Utbetalingstidslinje> {
        this.subsumsjonslogg = subsumsjonslogg
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

    override fun visit(dag: Utbetalingsdag.ForeldetDag, dato: LocalDate, økonomi: Økonomi) {}

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
        state.oppholdsdag(this, dato)
    }

    override fun visit(
        dag: Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        state.oppholdsdag(this, dato)
    }

    override fun visit(dag: Utbetalingsdag.ArbeidsgiverperiodedagNav, dato: LocalDate, økonomi: Økonomi) {
        state.oppholdsdag(this, dato)
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
    }

    override fun visit(
        dag: UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        state.oppholdsdag(this, dato)
    }

    private fun økOppholdstelling(dato: LocalDate) {
        sisteVurdering = sisteVurdering.økOppholdstelling(dato)
    }

    private fun subsummerTilstrekkeligOppholdNådd(dagen: LocalDate, oppholdFørDagen: Int = sisteVurdering.oppholdsteller): Boolean {
        // Nok opphold? 🤔
        val harTilstrekkeligOpphold = oppholdFørDagen >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER
        subsumsjonslogg.`§ 8-12 ledd 2`(
            oppfylt = harTilstrekkeligOpphold,
            dato = dagen,
            gjenståendeSykepengedager = sisteVurdering.gjenståendeDagerUnder67År(alder, arbeidsgiverRegler),
            beregnetAntallOppholdsdager = oppholdFørDagen,
            tilstrekkeligOppholdISykedager = TILSTREKKELIG_OPPHOLD_I_SYKEDAGER,
            tidslinjegrunnlag = tidslinjegrunnlagsubsumsjon,
            beregnetTidslinje = beregnetTidslinjesubsumsjon,
        )
        return harTilstrekkeligOpphold
    }

    private fun håndterBetalbarDag(dagen: LocalDate) {
        sisteVurdering = sisteVurdering.inkrementer(dagen)
        when {
            sisteVurdering.erDagerUnder67ÅrForbrukte(alder, arbeidsgiverRegler) -> state(Karantene(Begrunnelse.SykepengedagerOppbrukt))
            sisteVurdering.erDagerOver67ÅrForbrukte(alder, arbeidsgiverRegler) -> state(Karantene(Begrunnelse.SykepengedagerOppbruktOver67))
            else -> state(Syk)
        }
    }
    private fun håndterBetalbarDagEtterFerie(dagen: LocalDate) {
        håndterBetalbarDag(dagen)
    }
    private fun håndterBetalbarDagEtterOpphold(dagen: LocalDate) {
        val oppholdFørDagen = sisteVurdering.oppholdsteller
        sisteVurdering = sisteVurdering.dekrementer(dagen, dagen.minusYears(HISTORISK_PERIODE_I_ÅR))
        subsummerTilstrekkeligOppholdNådd(dagen, oppholdFørDagen = oppholdFørDagen)
        håndterBetalbarDag(dagen)
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
                avgrenser.sisteVurdering = Maksdatokontekst.TomKontekst
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                /* starter en helt ny maksdatosak 😊 */
                avgrenser.sisteVurdering = Maksdatokontekst(
                    vurdertTilOgMed = dagen,
                    startdatoSykepengerettighet = dagen,
                    startdatoTreårsvindu = dagen.minusYears(HISTORISK_PERIODE_I_ÅR),
                    betalteDager = setOf(dagen),
                    oppholdsteller = 0
                )
                avgrenser.state(Syk)
            }
        }

        object Syk : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                check(avgrenser.sisteVurdering.oppholdsteller == 0)
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.håndterBetalbarDag(dagen)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                avgrenser.state(Opphold)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                avgrenser.state(OppholdFri)
            }
        }

        object Opphold : State {

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterOpphold(dagen)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                oppholdsdag(avgrenser, dagen)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                if (!avgrenser.subsummerTilstrekkeligOppholdNådd(dagen)) return
                avgrenser.state(Initiell)
            }
        }

        object OppholdFri : State {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterFerie(dagen)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                if (!avgrenser.subsummerTilstrekkeligOppholdNådd(dagen)) return
                avgrenser.state(Initiell)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                if (!avgrenser.subsummerTilstrekkeligOppholdNådd(dagen)) return avgrenser.state(Opphold)
                avgrenser.state(Initiell)
            }
        }

        class Karantene(private val begrunnelse: Begrunnelse) : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                check(avgrenser.sisteVurdering.oppholdsteller == 0)
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.avvisDag(dagen, begrunnelse)
                avgrenser.økOppholdstelling(dagen)
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            override fun avvistDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.avvisDag(dagen, begrunnelse)
                avgrenser.økOppholdstelling(dagen)
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                /* helg skal ikke medføre ny rettighet */
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                if (!avgrenser.subsummerTilstrekkeligOppholdNådd(dagen)) return
                avgrenser.state(Initiell)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                /* helg skal ikke medføre ny rettighet */
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            private fun vurderTilstrekkeligOppholdNådd(avgrenser: MaksimumSykepengedagerfilter) {
                if (avgrenser.sisteVurdering.oppholdsteller < TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return
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
