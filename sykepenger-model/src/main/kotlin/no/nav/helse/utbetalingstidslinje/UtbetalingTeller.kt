package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-3 ledd 1 punktum 2`
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

    internal fun påGrensen(dato: LocalDate, doStuff: () -> Unit = {}): Boolean {
        val harNåddMaksSykepengedager = betalteDager >= arbeidsgiverRegler.maksSykepengedager()
        if (harNåddMaksSykepengedager) doStuff()

        val harNåddMaksSykepengedagerOver67 = gammelpersonDager >= arbeidsgiverRegler.maksSykepengedagerOver67()
        val harFylt70 = dato.plusDays(1) >= alder.datoForØvreAldersgrense
        //TODO: Aktivitetslogg().`§8-12 ledd 1`(oppfylt = !harNåddMaksSykepengedager)
        //TODO: Aktivitetslogg().`§8-51 ledd 3`(oppfylt = !harNåddMaksSykepengedagerOver67)
        Aktivitetslogg().`§8-3 ledd 1 punktum 2`(oppfylt = !harFylt70)
        return harNåddMaksSykepengedager
            || harNåddMaksSykepengedagerOver67
            || harFylt70
    }

    internal fun maksdato(sisteUtbetalingsdag: LocalDate) =
        beregnGjenståendeSykepengedager(minOf(alder.sisteVirkedagFørFylte70år, sisteUtbetalingsdag)).let { (_, maksdato) -> maksdato }

    internal fun forbrukteDager() = betalteDager

    internal fun gjenståendeSykepengedager(sisteUtbetalingsdag: LocalDate) =
        beregnGjenståendeSykepengedager(sisteUtbetalingsdag).let { (gjenståendeSykedager, _) -> gjenståendeSykedager }

    private fun beregnGjenståendeSykepengedager(sisteUtbetalingsdag: LocalDate): Pair<Int, LocalDate> {
        val clone = UtbetalingTeller(fom, alder, arbeidsgiverRegler, betalteDager, gammelpersonDager, aktivitetslogg)
        var result = sisteUtbetalingsdag
        var teller = 0
        while (!clone.påGrensen(result)) {
            result = result.plusDays(
                when (result.dayOfWeek) {
                    DayOfWeek.FRIDAY -> 3
                    DayOfWeek.SATURDAY -> 2
                    else -> 1
                }
            )
            teller += 1
            clone.inkrementer(result)
        }
        return teller to result
    }
}
