package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.etterlevelse.Tidslinjedag
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.plus
import no.nav.helse.ukedager
import no.nav.helse.utbetalingstidslinje.Maksdatosituasjon.Maksdatobestemmelse
import kotlin.math.max

internal class UtbetalingTeller(
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler
) {
    private companion object {
        private const val HISTORISK_PERIODE_I_ÅR: Long = 3
    }
    private var forbrukteDager: Int = 0
    private var gammelPersonDager: Int = 0
    private var startdatoSykepengerettighet: LocalDate = LocalDate.MIN
    private var startdatoTreårsvindu: LocalDate = LocalDate.MIN
    private val betalteDager = mutableSetOf<LocalDate>()

    internal fun inkrementer(dato: LocalDate) {
        if (forbrukteDager == 0) {
            startdatoSykepengerettighet = dato
            startdatoTreårsvindu = dato.minusYears(HISTORISK_PERIODE_I_ÅR)
        }
        betalteDager.add(dato)
        forbrukteDager += 1
        if (alder.innenfor67årsgrense(dato)) return
        gammelPersonDager += 1
    }

    internal fun dekrementer(dato: LocalDate) {
        val nyStartdatoTreårsvindu = dato.minusYears(HISTORISK_PERIODE_I_ÅR)
        if (nyStartdatoTreårsvindu >= startdatoSykepengerettighet) {
            startdatoTreårsvindu.datesUntil(nyStartdatoTreårsvindu).filter { it in betalteDager }.forEach { dekrementer ->
                dekrementerDag(dekrementer)
            }
        }
        startdatoTreårsvindu = nyStartdatoTreårsvindu
    }

    private fun dekrementerDag(dato: LocalDate) {
        if (dato < startdatoSykepengerettighet) return
        forbrukteDager = max(0, forbrukteDager - 1)
    }

    internal fun resett() {
        betalteDager.clear()
        startdatoSykepengerettighet = LocalDate.MIN
        startdatoTreårsvindu = LocalDate.MIN
        forbrukteDager = 0
        gammelPersonDager = 0
    }

    fun maksdatosituasjon(dato: LocalDate) =
        Maksdatosituasjon(arbeidsgiverRegler, dato, alder, forbrukteDager, gammelPersonDager, startdatoSykepengerettighet, startdatoTreårsvindu, betalteDager.toSet())
}

internal class Maksdatosituasjon(
    regler: ArbeidsgiverRegler,
    private val dato: LocalDate,
    alder: Alder,
    internal val forbrukteDager: Int,
    private val forbrukteDagerOver67: Int,
    startdatoSykepengerettighet: LocalDate,
    private val startdatoTreårsvindu: LocalDate,
    private val betalteDager: Set<LocalDate>
) {
    private val redusertYtelseAlder = alder.redusertYtelseAlder
    private val syttiårsdagen = alder.syttiårsdagen
    private val sisteVirkedagFørFylte70år = syttiårsdagen.forrigeVirkedagFør()

    private val gjenståendeSykepengedagerUnder67 = regler.maksSykepengedager() - forbrukteDager

    private val gjenståendeSykepengedagerOver67 = regler.maksSykepengedagerOver67() - forbrukteDagerOver67
    private val startdatoSykepengerettighet = startdatoSykepengerettighet.takeUnless { it == LocalDate.MIN }

    private val harNåddMaks = minOf(gjenståendeSykepengedagerOver67, gjenståendeSykepengedagerUnder67) == 0
    private val forrigeMaksdato = if (harNåddMaks) betalteDager.last() else null
    private val forrigeVirkedag = forrigeMaksdato ?: dato.sisteVirkedagInklusiv()

    private val maksdatoOrdinærRett = forrigeVirkedag + gjenståendeSykepengedagerUnder67.ukedager
    private val maksdatoBegrensetRett = maxOf(forrigeVirkedag, redusertYtelseAlder) + gjenståendeSykepengedagerOver67.ukedager

    private val hjemmelsbegrunnelse: Maksdatobestemmelse
    internal val maksdato: LocalDate
    internal val gjenståendeDager: Int

    init {
        // maksdato er den dagen som først inntreffer blant ordinær kvote, 67-års-kvoten og 70-årsdagen,
        // med mindre man allerede har brukt opp alt tidligere
        when {
            maksdatoOrdinærRett <= maksdatoBegrensetRett -> {
                maksdato = maksdatoOrdinærRett
                gjenståendeDager = gjenståendeSykepengedagerUnder67
                hjemmelsbegrunnelse = Maksdatobestemmelse.OrdinærRett
            }
            maksdatoBegrensetRett <= sisteVirkedagFørFylte70år -> {
                maksdato = maksdatoBegrensetRett
                gjenståendeDager = ukedager(forrigeVirkedag, maksdato)
                hjemmelsbegrunnelse = Maksdatobestemmelse.BegrensetRett
            }
            else -> {
                maksdato = sisteVirkedagFørFylte70år
                gjenståendeDager = ukedager(forrigeVirkedag, maksdato)
                hjemmelsbegrunnelse = Maksdatobestemmelse.Over70
            }
        }
    }

    internal fun erDagerUnder67ÅrForbrukte() = gjenståendeSykepengedagerUnder67 == 0
    internal fun erDagerOver67ÅrForbrukte() = gjenståendeSykepengedagerOver67 == 0

    internal fun vurderMaksdatobestemmelse(
        subsumsjonObserver: SubsumsjonObserver,
        periode: Periode,
        tidslinjegrunnlagsubsumsjon: List<List<Tidslinjedag>>,
        beregnetTidslinjesubsumsjon: List<Tidslinjedag>,
        avvisteDager: Set<LocalDate>
    ) {
        hjemmelsbegrunnelse.sporHjemmel(
            subsumsjonObserver,
            periode,
            tidslinjegrunnlagsubsumsjon,
            beregnetTidslinjesubsumsjon,
            avvisteDager,
            this
        )
    }

    fun vurderHarTilstrekkeligOpphold(subsumsjonObserver: SubsumsjonObserver, opphold: Int, tilstrekkeligOpphold: Int, tidslinjegrunnlagsubsumsjon: List<List<Tidslinjedag>>, beregnetTidslinjesubsumsjon: List<Tidslinjedag>): Boolean {
        val harTilstrekkeligOpphold = opphold >= tilstrekkeligOpphold
        subsumsjonObserver.`§ 8-12 ledd 2`(
            oppfylt = harTilstrekkeligOpphold,
            dato = dato,
            gjenståendeSykepengedager = gjenståendeSykepengedagerUnder67,
            beregnetAntallOppholdsdager = opphold,
            tilstrekkeligOppholdISykedager = tilstrekkeligOpphold,
            tidslinjegrunnlag = tidslinjegrunnlagsubsumsjon,
            beregnetTidslinje = beregnetTidslinjesubsumsjon,
        )
        return harTilstrekkeligOpphold
    }

    private fun førFylte70(subsumsjonObserver: SubsumsjonObserver, periode: Periode, utfallTom: LocalDate = periode.endInclusive) {
        subsumsjonObserver.`§ 8-3 ledd 1 punktum 2`(
            oppfylt = true,
            syttiårsdagen = syttiårsdagen,
            utfallFom = periode.start,
            utfallTom = utfallTom,
            tidslinjeFom = periode.start,
            tidslinjeTom = periode.endInclusive,
            avvistePerioder = emptyList()
        )
    }

    private fun interface Maksdatobestemmelse {
        fun sporHjemmel(
            subsumsjonObserver: SubsumsjonObserver,
            periode: Periode,
            tidslinjegrunnlagsubsumsjon: List<List<Tidslinjedag>>,
            beregnetTidslinjesubsumsjon: List<Tidslinjedag>,
            avvisteDager: Set<LocalDate>,
            maksdatosituasjon: Maksdatosituasjon
        )

        companion object {
            val OrdinærRett = Maksdatobestemmelse { subsumsjonObserver, periode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, _, maksdatosituasjon ->
                subsumsjonObserver.`§ 8-12 ledd 1 punktum 1`(periode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, maksdatosituasjon.gjenståendeDager, maksdatosituasjon.forbrukteDager, maksdatosituasjon.maksdato, maksdatosituasjon.startdatoSykepengerettighet)
                maksdatosituasjon.førFylte70(subsumsjonObserver, periode)
            }
            val BegrensetRett = Maksdatobestemmelse { subsumsjonObserver, periode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, _, maksdatosituasjon ->
                subsumsjonObserver.`§ 8-51 ledd 3`(periode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, maksdatosituasjon.gjenståendeDager, maksdatosituasjon.forbrukteDager, maksdatosituasjon.maksdato, maksdatosituasjon.startdatoSykepengerettighet)
                maksdatosituasjon.førFylte70(subsumsjonObserver, periode)
            }
            val Over70 = Maksdatobestemmelse { subsumsjonObserver, periode, _, _, avvisteDager, maksdatosituasjon ->
                if (periode.start < maksdatosituasjon.syttiårsdagen) {
                    maksdatosituasjon.førFylte70(subsumsjonObserver, periode, maksdatosituasjon.syttiårsdagen.forrigeDag)
                }

                val avvisteDagerFraOgMedSøtti = avvisteDager.filter { it >= maksdatosituasjon.syttiårsdagen }
                if (avvisteDagerFraOgMedSøtti.isEmpty()) return@Maksdatobestemmelse
                subsumsjonObserver.`§ 8-3 ledd 1 punktum 2`(
                    oppfylt = false,
                    syttiårsdagen = maksdatosituasjon.syttiårsdagen,
                    utfallFom = maxOf(maksdatosituasjon.syttiårsdagen, periode.start),
                    utfallTom = periode.endInclusive,
                    tidslinjeFom = periode.start,
                    tidslinjeTom = periode.endInclusive,
                    avvistePerioder = avvisteDagerFraOgMedSøtti.grupperSammenhengendePerioder()
                )
            }
        }
    }

    private companion object {
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
    }
}
