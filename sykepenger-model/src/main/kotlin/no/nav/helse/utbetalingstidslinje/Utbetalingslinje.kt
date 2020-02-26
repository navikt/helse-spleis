package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import java.time.LocalDate

data class Utbetalingslinje(
    val fom: LocalDate,
    var tom: LocalDate,
    val dagsats: Int
) {
//    internal fun toTidslinje() = Utbetalingstidslinje().apply {
//        fom.datesUntil(tom.plusDays(1)).forEach {
//            if (it.erHelg()) this.addHelg(0.0, it) else this.addNAVdag(dagsats.toDouble(), it)
//        }
//    }
}

//internal fun List<Utbetalingslinje>.utbetalingstidslinje() = this
//    .map { it.toTidslinje() }
//    .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

/**
 * Oppdrag expects a continuous payment timeline spanning weekends.
 * They need to know about the weekends for tax calculation, but have rules for not paying for them
 */
internal fun List<Utbetalingslinje>.joinForOppdrag(): List<Utbetalingslinje> {
    fun Utbetalingslinje.tilstøtende(utbetalingslinje: Utbetalingslinje) =
        this.tom.dayOfWeek == DayOfWeek.FRIDAY && this.tom.plusDays(3).isEqual(utbetalingslinje.fom)

    if (this.isEmpty()) return this
    val results = mutableListOf(this.first())
    for (utbetalingslinje: Utbetalingslinje in this.slice(1 until this.size)) {
        if (results.last().tilstøtende(utbetalingslinje)) {
            require(results.last().dagsats == utbetalingslinje.dagsats) { "Uventet dagsats - forventet samme" }
            results[results.size - 1] =
                Utbetalingslinje(
                    results.last().fom,
                    utbetalingslinje.tom,
                    results.last().dagsats
                )
        } else {
            results.add(utbetalingslinje)
        }
    }
    return results.toList()
}
