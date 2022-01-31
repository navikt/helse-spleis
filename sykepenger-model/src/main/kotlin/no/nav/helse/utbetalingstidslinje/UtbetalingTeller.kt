package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Begrunnelse.SykepengedagerOppbrukt
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max

internal class UtbetalingTeller private constructor(
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private var forbrukteDager: Int,
    private var gammelPersonDager: Int,
    private val subsumsjonObserver: SubsumsjonObserver
) {
    private companion object {
        private const val HISTORISK_PERIODE_I_ÅR: Long = 3

    }
    private lateinit var startdatoSykepengerettighet: LocalDate
    private lateinit var startdatoTreårsvindu: LocalDate
    private val betalteDager = mutableSetOf<LocalDate>()

    internal constructor(alder: Alder, arbeidsgiverRegler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver) :
        this(alder, arbeidsgiverRegler, 0, 0, subsumsjonObserver)


    internal fun begrunnelse(dato: LocalDate): Begrunnelse {
        // avslag skal begrunnes med SykepengedagerOppbrukt (§ 8-15) så lenge man har brukt ordinær kvote;
        // uavhengig om man er over eller under 67
        if (forbrukteDager >= arbeidsgiverRegler.maksSykepengedager()) return SykepengedagerOppbrukt
        return alder.begrunnelseForAlder(dato)
    }

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

    private var påGrensenTilstand: PåGrensenTilstand = object : PåGrensenTilstand {}

    private interface PåGrensenTilstand {
        fun hvis248Dager(block: (LocalDate) -> Unit = {}) {}
        fun hvis67År(block: (LocalDate) -> Unit = {}) {}
        fun hvis70År(block: (LocalDate) -> Unit = {}) {}
    }

    private class Over248Tilstand(private val maksdato: LocalDate) : PåGrensenTilstand {
        override fun hvis248Dager(block: (LocalDate) -> Unit) {
            block(maksdato)
        }
    }

    private class Over67Tilstand(private val maksdato: LocalDate) : PåGrensenTilstand {
        override fun hvis67År(block: (LocalDate) -> Unit) {
            block(maksdato)
        }
    }

    private class Over70Tilstand(private val maksdato: LocalDate) : PåGrensenTilstand {
        override fun hvis70År(block: (LocalDate) -> Unit) {
            block(maksdato)
        }
    }

    internal fun hvisGrensenErNådd(
        hvis248Dager: (maksdato: LocalDate) -> Unit = {},
        hvis67År: (maksdato: LocalDate) -> Unit = {},
        hvis70År: (maksdato: LocalDate) -> Unit = {}
    ) {
        påGrensenTilstand.hvis248Dager(hvis248Dager)
        påGrensenTilstand.hvis67År(hvis67År)
        påGrensenTilstand.hvis70År(hvis70År)
    }

    internal fun `§8-51 ledd 3`(maksdato: LocalDate) {
        subsumsjonObserver.`§8-51 ledd 3`(
            oppfylt = false,
            maksSykepengedagerOver67 = arbeidsgiverRegler.maksSykepengedagerOver67(),
            gjenståendeSykedager = 0,
            forbrukteSykedager = forbrukteDager,
            maksdato = maksdato
        )
    }

    private fun `§8-51 ledd 3 - beregning`(forbrukteSykedager: Int, gjenståendeSykedager: Int, maksdato: LocalDate) {
        subsumsjonObserver.`§8-51 ledd 3`(
            oppfylt = true,
            maksSykepengedagerOver67 = arbeidsgiverRegler.maksSykepengedagerOver67(),
            gjenståendeSykedager = gjenståendeSykedager,
            forbrukteSykedager = forbrukteSykedager,
            maksdato = maksdato
        )
    }

    internal fun erFørMaksdato(dato: LocalDate): Boolean {
        val harNåddMaksSykepengedager = forbrukteDager >= arbeidsgiverRegler.maksSykepengedager()
        val harNåddMaksSykepengedagerOver67 = gammelPersonDager >= arbeidsgiverRegler.maksSykepengedagerOver67()
        val harFylt70 = alder.harNådd70årsgrense(dato)
        //TODO: Aktivitetslogg().`§8-12 ledd 1`(oppfylt = !harNåddMaksSykepengedager)
        when {
            harFylt70 -> påGrensenTilstand = Over70Tilstand(dato)
            harNåddMaksSykepengedagerOver67 -> påGrensenTilstand = Over67Tilstand(dato)
            harNåddMaksSykepengedager -> påGrensenTilstand = Over248Tilstand(dato)
        }
        return !harNåddMaksSykepengedager && !harNåddMaksSykepengedagerOver67 && !harFylt70
    }

    private var forrigeResultat: Pair<Int, LocalDate>? = null

    internal fun maksdato(sisteUtbetalingsdag: LocalDate): LocalDate {
        beregnGjenståendeSykepengedager(minOf(alder.sisteVirkedagFørFylte70år, sisteUtbetalingsdag))
        return forrigeResultat!!.let { (_, maksdato) -> maksdato }
    }

    internal fun forbrukteDager() = forbrukteDager

    internal fun gjenståendeSykepengedager(sisteUtbetalingsdag: LocalDate): Int {
        beregnGjenståendeSykepengedager(sisteUtbetalingsdag)
        return forrigeResultat!!.let { (gjenståendeSykedager, _) -> gjenståendeSykedager }
    }

    private fun beregnGjenståendeSykepengedager(sisteUtbetalingsdag: LocalDate) {
        val faktiskeDagerTeller = this
        val gjenståendeDagerTeller = UtbetalingTeller(alder, arbeidsgiverRegler, forbrukteDager, gammelPersonDager, subsumsjonObserver)
        var result = sisteUtbetalingsdag
        var teller = 0
        while (gjenståendeDagerTeller.erFørMaksdato(result)) {
            result = result.plusDays(
                when (result.dayOfWeek) {
                    DayOfWeek.FRIDAY -> 3
                    DayOfWeek.SATURDAY -> 2
                    else -> 1
                }
            )
            teller += 1
            gjenståendeDagerTeller.inkrementer(result)
        }
        val nyttResultat = teller to result
        if (nyttResultat != faktiskeDagerTeller.forrigeResultat) {
            faktiskeDagerTeller.forrigeResultat = nyttResultat
            gjenståendeDagerTeller.hvisGrensenErNådd(
                hvis67År = {
                    gjenståendeDagerTeller.`§8-51 ledd 3 - beregning`(faktiskeDagerTeller.forbrukteDager, teller, result)
                }
            )
        }
    }
}
