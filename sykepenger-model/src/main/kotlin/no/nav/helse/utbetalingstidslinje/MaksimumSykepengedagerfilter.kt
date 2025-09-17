package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.Tidslinjedag
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.etterlevelse.`§ 8-12 ledd 1 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-12 ledd 2`
import no.nav.helse.etterlevelse.`§ 8-3 ledd 1 punktum 2`
import no.nav.helse.etterlevelse.`§ 8-51 ledd 3`
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_9
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter.State.Karantene
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter.State.Syk
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag

internal class MaksimumSykepengedagerfilter(
    private val alder: Alder,
    private val subsumsjonslogg: Subsumsjonslogg,
    private val aktivitetslogg: IAktivitetslogg,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val infotrygdtidslinje: Utbetalingstidslinje
) : UtbetalingstidslinjerFilter {

    internal companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26 * 7
        private const val HISTORISK_PERIODE_I_ÅR: Long = 3
    }

    internal val maksdatosaker = mutableListOf<Maksdatokontekst>()
    private var sisteVurdering = Maksdatokontekst.TomKontekst

    private var state: State = State.Initiell
    private lateinit var beregnetTidslinje: Utbetalingstidslinje
    private lateinit var tidslinjegrunnlag: List<Utbetalingstidslinje>

    private val tidslinjegrunnlagsubsumsjon by lazy { tidslinjegrunnlag.subsumsjonsformat() }
    private val beregnetTidslinjesubsumsjon by lazy { beregnetTidslinje.subsumsjonsformat() }
    internal fun maksdatoresultatForVedtaksperiode(periode: Periode): Maksdatovurdering {
        return Maksdatovurdering(
            resultat = sisteVurdering
                .avgrensTil(periode.endInclusive)
                .beregnMaksdato(alder, arbeidsgiverRegler),
            tidslinjegrunnlagsubsumsjon = tidslinjegrunnlagsubsumsjon,
            beregnetTidslinjesubsumsjon = beregnetTidslinjesubsumsjon,
            syttiårsdag = alder.syttiårsdagen
        )
    }

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        tidslinjegrunnlag = arbeidsgivere.map { it.samletVedtaksperiodetidslinje } + listOf(infotrygdtidslinje.fremTilOgMed(vedtaksperiode.endInclusive))
        beregnetTidslinje = tidslinjegrunnlag.reduce(Utbetalingstidslinje::plus)

        Utbetalingstidslinje.periode(tidslinjegrunnlag)
            ?.forEach { dato ->
                when (val dag = beregnetTidslinje[dato]) {
                    is Utbetalingsdag.Arbeidsdag -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.ArbeidsgiverperiodeDag -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.ArbeidsgiverperiodedagNav -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.AvvistDag -> state.avvistDag(this, dag.dato)
                    is Utbetalingsdag.ForeldetDag -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.Fridag -> state.fridag(this, dag.dato)
                    is NavDag -> {
                        if (alder.mistetSykepengerett(dag.dato)) state(State.ForGammel)
                        state.betalbarDag(this, dag.dato)
                    }

                    is Utbetalingsdag.NavHelgDag -> {
                        if (alder.mistetSykepengerett(dag.dato)) state(State.ForGammel)
                        state.sykdomshelg(this, dag.dato)
                    }

                    is UkjentDag -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.Ventetidsdag -> state.oppholdsdag(this, dag.dato)
                }
            }

        /** går gjennom alle maksdato-sakene og avslår dager. EGENTLIG er det nok å avslå dagene
         *  fra sisteVurdering, men det er noen enhetstester som tester veldig lange
         *  tidslinjer og de forventer at alle maksdatodager avslås, uavhengig av maksdatosak
         */
        val begrunnelser = (maksdatosaker.plusElement(sisteVurdering))
            .flatMap { maksdatosak -> maksdatosak.begrunnelseForAvslåtteDager(alder, arbeidsgiverRegler, TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        val avvisteTidslinjer = begrunnelser.entries.fold(arbeidsgivere) { result, (begrunnelse, dager) ->
            result.avvis(dager.grupperSammenhengendePerioder(), begrunnelse)
        }

        if (sisteVurdering.fremdelesSykEtterTilstrekkeligOpphold(vedtaksperiode, TILSTREKKELIG_OPPHOLD_I_SYKEDAGER)) {
            aktivitetslogg.funksjonellFeil(RV_VV_9)
        }
        if (sisteVurdering.harNåddMaks(vedtaksperiode))
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

    private fun økOppholdstelling(dato: LocalDate) {
        sisteVurdering = sisteVurdering.medOppholdsdag(dato)
    }

    private fun subsummerTilstrekkeligOppholdNådd(dagen: LocalDate, oppholdFørDagen: Int = sisteVurdering.oppholdsteller): Boolean {
        // Nok opphold? 🤔
        val harTilstrekkeligOpphold = oppholdFørDagen >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER
        val gjenståendeSykepengedager = sisteVurdering.gjenståendeDagerUnder67År(arbeidsgiverRegler)
        // Bare relevant om det er ny rett på sykepenger eller om vilkåret ikke er oppfylt
        if (harTilstrekkeligOpphold || gjenståendeSykepengedager == 0) {
            subsumsjonslogg.logg(
                `§ 8-12 ledd 2`(
                    oppfylt = harTilstrekkeligOpphold,
                    dato = dagen,
                    gjenståendeSykepengedager = gjenståendeSykepengedager,
                    beregnetAntallOppholdsdager = oppholdFørDagen,
                    tilstrekkeligOppholdISykedager = TILSTREKKELIG_OPPHOLD_I_SYKEDAGER,
                    tidslinjegrunnlag = tidslinjegrunnlagsubsumsjon,
                    beregnetTidslinje = beregnetTidslinjesubsumsjon,
                )
            )
        }
        return harTilstrekkeligOpphold
    }

    private fun håndterBetalbarDag(dagen: LocalDate) {
        sisteVurdering = sisteVurdering.inkrementer(dagen)
        when {
            sisteVurdering.erDagerUnder67ÅrForbrukte(arbeidsgiverRegler) || sisteVurdering.erDagerOver67ÅrForbrukte(alder, arbeidsgiverRegler) -> state(Karantene)
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

    private fun håndterBetalbarDagEtterMaksdato(dag: LocalDate) {
        sisteVurdering = sisteVurdering.medAvslåttDag(dag)
    }

    private interface State {
        fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate)
        fun avvistDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) = oppholdsdag(avgrenser, dagen)
        fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate)
        fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate)
        fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) = oppholdsdag(avgrenser, dagen)
        fun entering(avgrenser: MaksimumSykepengedagerfilter) {}
        fun leaving(avgrenser: MaksimumSykepengedagerfilter) {}

        object Initiell : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.maksdatosaker.add(avgrenser.sisteVurdering)
                avgrenser.sisteVurdering = Maksdatokontekst.TomKontekst
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                /* starter en helt ny maksdatosak 😊 */
                avgrenser.sisteVurdering = Maksdatokontekst(
                    vurdertTilOgMed = dagen,
                    startdatoSykepengerettighet = dagen,
                    startdatoTreårsvindu = dagen.minusYears(HISTORISK_PERIODE_I_ÅR),
                    betalteDager = setOf(dagen),
                    oppholdsdager = emptySet(),
                    avslåtteDager = emptySet()
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

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                /* verken forbrukt dag eller oppholdsdag 😌 */
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

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                /* verken forbrukt dag eller oppholdsdag 😌 */
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

        object Karantene : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                check(avgrenser.sisteVurdering.oppholdsteller == 0)
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            override fun avvistDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
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
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
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

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
            override fun leaving(avgrenser: MaksimumSykepengedagerfilter) = throw IllegalStateException("Kan ikke gå ut fra state ForGammel")
            private fun over70(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
            }
        }
    }
}

data class Maksdatovurdering(
    val resultat: Maksdatoresultat,
    val tidslinjegrunnlagsubsumsjon: List<List<Tidslinjedag>>,
    val beregnetTidslinjesubsumsjon: List<Tidslinjedag>,
    val syttiårsdag: LocalDate
) {
    fun subsummer(subsumsjonslogg: Subsumsjonslogg, vedtaksperiode: Periode) {
        val førSyttiårsdagen = fun(subsumsjonslogg: Subsumsjonslogg, utfallTom: LocalDate) {
            subsumsjonslogg.logg(
                `§ 8-3 ledd 1 punktum 2`(
                    oppfylt = true,
                    syttiårsdagen = syttiårsdag,
                    utfallFom = vedtaksperiode.start,
                    utfallTom = utfallTom,
                    tidslinjeFom = vedtaksperiode.start,
                    tidslinjeTom = vedtaksperiode.endInclusive,
                    avvistePerioder = emptyList()
                )
            )
        }

        when (resultat.bestemmelse) {
            Maksdatoresultat.Bestemmelse.IKKE_VURDERT -> error("ugyldig situasjon ${resultat.bestemmelse}")
            Maksdatoresultat.Bestemmelse.ORDINÆR_RETT -> {
                `§ 8-12 ledd 1 punktum 1`(vedtaksperiode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, resultat.gjenståendeDager, resultat.antallForbrukteDager, resultat.maksdato, resultat.startdatoSykepengerettighet ?: LocalDate.MIN).forEach {
                    subsumsjonslogg.logg(it)
                }
                førSyttiårsdagen(subsumsjonslogg, vedtaksperiode.endInclusive)
            }

            Maksdatoresultat.Bestemmelse.BEGRENSET_RETT -> {
                `§ 8-51 ledd 3`(vedtaksperiode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, resultat.gjenståendeDager, resultat.antallForbrukteDager, resultat.maksdato, resultat.startdatoSykepengerettighet ?: LocalDate.MIN).forEach {
                    subsumsjonslogg.logg(it)
                }
                førSyttiårsdagen(subsumsjonslogg, syttiårsdag.forrigeDag)
            }

            Maksdatoresultat.Bestemmelse.SYTTI_ÅR -> {
                if (vedtaksperiode.start < syttiårsdag) {
                    førSyttiårsdagen(subsumsjonslogg, syttiårsdag.forrigeDag)
                }

                val avvisteDagerFraOgMedSøtti = resultat.avslåtteDager.flatten().filter { it >= syttiårsdag }
                if (avvisteDagerFraOgMedSøtti.isNotEmpty()) {
                    subsumsjonslogg.logg(
                        `§ 8-3 ledd 1 punktum 2`(
                            oppfylt = false,
                            syttiårsdagen = syttiårsdag,
                            utfallFom = maxOf(syttiårsdag, vedtaksperiode.start),
                            utfallTom = vedtaksperiode.endInclusive,
                            tidslinjeFom = vedtaksperiode.start,
                            tidslinjeTom = vedtaksperiode.endInclusive,
                            avvistePerioder = avvisteDagerFraOgMedSøtti.grupperSammenhengendePerioder()
                        )
                    )
                }
            }
        }
    }
}
