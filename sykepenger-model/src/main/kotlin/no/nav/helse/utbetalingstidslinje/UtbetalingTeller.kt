package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-51 ledd 3`
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Begrunnelse.SykepengedagerOppbrukt
import no.nav.helse.utbetalingstidslinje.Begrunnelse.SykepengedagerOppbruktOver67
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max

internal class UtbetalingTeller private constructor(
    private var fom: LocalDate,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private var betalteDager: Int,
    private var gammelpersonDager: Int,
    private val aktivitetslogg: IAktivitetslogg
) {
    // Til bruk under visitering av Utbetalingstidslinje
    internal var begrunnelse: Begrunnelse = SykepengedagerOppbrukt

    internal constructor(
        alder: Alder,
        arbeidsgiverRegler: ArbeidsgiverRegler,
        aktivitetslogg: IAktivitetslogg
    ) :
        this(LocalDate.MIN, alder, arbeidsgiverRegler, 0, 0, aktivitetslogg)

    private fun byttBegrunnelseFordiAntallGjenværendeDagerReduseresTil60EllerVedNyRettighet(): Boolean = gammelpersonDager == 0 && betalteDager < (248 - 60)

    internal fun inkrementer(dato: LocalDate) {
        betalteDager += 1
        if (dato > alder.redusertYtelseAlder) {
            if (byttBegrunnelseFordiAntallGjenværendeDagerReduseresTil60EllerVedNyRettighet()) {
                begrunnelse = SykepengedagerOppbruktOver67
            }
            gammelpersonDager += 1
        }
    }

    internal fun dekrementer(dato: LocalDate) {
        if (dato < fom) return
        betalteDager = max(0, betalteDager - 1)
        // gammelpersonDager kan ikke bli mer enn tre år gamle innen man fyller 70
    }

    internal fun resett(dato: LocalDate) {
        fom = dato
        betalteDager = 0
        gammelpersonDager = 0
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
        aktivitetslogg.`§8-51 ledd 3`(
            oppfylt = false,
            maksSykepengedagerOver67 = arbeidsgiverRegler.maksSykepengedagerOver67(),
            gjenståendeSykedager = 0,
            forbrukteSykedager = betalteDager,
            maksdato = maksdato
        )

    }

    private fun `§8-51 ledd 3 - beregning`(forbrukteSykedager: Int, gjenståendeSykedager: Int, maksdato: LocalDate) {
        aktivitetslogg.`§8-51 ledd 3`(
            oppfylt = true,
            maksSykepengedagerOver67 = arbeidsgiverRegler.maksSykepengedagerOver67(),
            gjenståendeSykedager = gjenståendeSykedager,
            forbrukteSykedager = forbrukteSykedager,
            maksdato = maksdato
        )
    }

    internal fun erFørMaksdato(dato: LocalDate): Boolean {
        val harNåddMaksSykepengedager = betalteDager >= arbeidsgiverRegler.maksSykepengedager()
        val harNåddMaksSykepengedagerOver67 = gammelpersonDager >= arbeidsgiverRegler.maksSykepengedagerOver67()
        val harFylt70 = dato.plusDays(1) >= alder.datoForØvreAldersgrense
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

    internal fun forbrukteDager() = betalteDager

    internal fun gjenståendeSykepengedager(sisteUtbetalingsdag: LocalDate): Int {
        beregnGjenståendeSykepengedager(sisteUtbetalingsdag)
        return forrigeResultat!!.let { (gjenståendeSykedager, _) -> gjenståendeSykedager }
    }

    private fun beregnGjenståendeSykepengedager(sisteUtbetalingsdag: LocalDate) {
        val faktiskeDagerTeller = this
        val gjenståendeDagerTeller = UtbetalingTeller(fom, alder, arbeidsgiverRegler, betalteDager, gammelpersonDager, aktivitetslogg)
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
                    gjenståendeDagerTeller.`§8-51 ledd 3 - beregning`(faktiskeDagerTeller.betalteDager, teller, result)
                }
            )
        }
    }
}
