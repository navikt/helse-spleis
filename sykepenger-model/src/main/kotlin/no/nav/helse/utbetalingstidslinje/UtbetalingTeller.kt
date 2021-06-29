package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.IAktivitetslogg
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
    internal constructor(
        alder: Alder,
        arbeidsgiverRegler: ArbeidsgiverRegler,
        aktivitetslogg: IAktivitetslogg
    ) :
        this(LocalDate.MIN, alder, arbeidsgiverRegler, 0, 0, aktivitetslogg)

    internal fun inkrementer(dato: LocalDate) {
        betalteDager += 1
        if (dato.isAfter(alder.redusertYtelseAlder)) gammelpersonDager += 1
    }

    internal fun dekrementer(dato: LocalDate) {
        if (dato < fom) return
        betalteDager = max(0, betalteDager - 1)
        if (dato.isAfter(alder.redusertYtelseAlder)) gammelpersonDager = max(0, gammelpersonDager - 1)
    }

    internal fun resett(dato: LocalDate) {
        fom = dato
        betalteDager = 0
        gammelpersonDager = 0
    }

    internal fun påGrensen(dato: LocalDate, erIkkeIGjenståendeSykedagerberegning: Boolean = true): Boolean {
        val harNåddMaksSykepengedager = betalteDager >= arbeidsgiverRegler.maksSykepengedager()
        if (harNåddMaksSykepengedager && erIkkeIGjenståendeSykedagerberegning) {
            aktivitetslogg.lovtrace.`§8-12 ledd 1`(false, "Allsang på Grensen - $dato")
        }
        return harNåddMaksSykepengedager
            || gammelpersonDager >= arbeidsgiverRegler.maksSykepengedagerOver67()
            || dato.plusDays(1) >= alder.øvreAldersgrense
    }

    internal fun maksdato(sisteUtbetalingsdag: LocalDate) =
        beregnGjenståendeSykedager(sisteUtbetalingsdag).let { (_, maksdato) -> maksdato }

    internal fun forbrukteDager() = betalteDager

    internal fun gjenståendeSykedager(sisteUtbetalingsdag: LocalDate) =
        beregnGjenståendeSykedager(sisteUtbetalingsdag).let { (gjenståendeSykedager, _) -> gjenståendeSykedager }

    private fun beregnGjenståendeSykedager(sisteUtbetalingsdag: LocalDate): Pair<Int, LocalDate> {
        val clone = UtbetalingTeller(fom, alder, arbeidsgiverRegler, betalteDager, gammelpersonDager, aktivitetslogg)
        var result = sisteUtbetalingsdag
        var teller = 0
        while (!clone.påGrensen(result, false)) {
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
