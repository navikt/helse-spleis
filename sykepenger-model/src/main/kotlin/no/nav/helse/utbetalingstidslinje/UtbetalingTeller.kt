package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max

internal class UtbetalingTeller private constructor(
    private var fom: LocalDate,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private var betalteDager: Int,
    private var gammelpersonDager: Int
) {
    internal constructor(
        alder: Alder,
        arbeidsgiverRegler: ArbeidsgiverRegler
    ) :
        this(LocalDate.MIN, alder, arbeidsgiverRegler, 0, 0)

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

    internal fun påGrensen(dato: LocalDate): Boolean {
        return betalteDager >= arbeidsgiverRegler.maksSykepengedager()
            || gammelpersonDager >= arbeidsgiverRegler.maksSykepengedagerOver67()
            || dato.plusDays(1) >= alder.øvreAldersgrense
    }

    internal fun maksdato(sisteUtbetalingsdag: LocalDate) =
        beregnGjenståendeSykedager(sisteUtbetalingsdag).let { (_, maksdato) -> maksdato }

    internal fun forbrukteDager() = betalteDager

    internal fun gjenståendeSykedager(sisteUtbetalingsdag: LocalDate) =
        beregnGjenståendeSykedager(sisteUtbetalingsdag).let { (gjenståendeSykedager, _) -> gjenståendeSykedager }

    private fun beregnGjenståendeSykedager(sisteUtbetalingsdag: LocalDate): Pair<Int, LocalDate> {
        val clone = UtbetalingTeller(fom, alder, arbeidsgiverRegler, betalteDager, gammelpersonDager)
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
