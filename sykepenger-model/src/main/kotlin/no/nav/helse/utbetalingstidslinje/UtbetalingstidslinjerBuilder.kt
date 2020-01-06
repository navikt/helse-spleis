package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal class UtbetalingstidslinjerBuilder(private val fom: LocalDate, private val tom: LocalDate, private val historiskUtbetalinger: List<HistoriskUtbetaling>) {

    internal fun results() : List<Utbetalingstidslinje> {
        return historiskUtbetalinger.map { it.toTidslinje(fom, tom) }
    }

}
