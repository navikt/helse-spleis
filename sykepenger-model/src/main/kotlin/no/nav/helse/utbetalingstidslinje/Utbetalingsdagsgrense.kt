package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.dag.erHelg
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max

internal class Utbetalingsdagsgrense private constructor(private var fom: LocalDate,
                                                         private val alder: Alder,
                                                         private val arbeidsgiverRegler: ArbeidsgiverRegler,
                                                         private var betalteDager: Int,
                                                         private var gammelpersonDager: Int) {


    internal constructor(fom: LocalDate,
                        alder: Alder,
                        arbeidsgiverRegler: ArbeidsgiverRegler) :
        this(fom, alder, arbeidsgiverRegler, 0, 0)

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
        return betalteDager >= arbeidsgiverRegler.maksSykepengedager() || gammelpersonDager >= 60 || dato.plusDays(1) >= alder.øvreAldersgrense
    }

    internal fun maksdato(sisteUtbetalingsdag: LocalDate): LocalDate {
        val clone = Utbetalingsdagsgrense(fom, alder, arbeidsgiverRegler, betalteDager, gammelpersonDager)
        var result = sisteUtbetalingsdag
        while (!clone.påGrensen(result)) {
            result = result.plusDays(if (result.dayOfWeek == DayOfWeek.FRIDAY) 3 else 1)
            clone.inkrementer(result)
        }
        return result
    }
}
