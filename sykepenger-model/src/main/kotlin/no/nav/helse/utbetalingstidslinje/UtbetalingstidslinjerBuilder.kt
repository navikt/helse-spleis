package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal class UtbetalingstidslinjerBuilder(private val fom: LocalDate, private val tom: LocalDate, private val historiskUtbetalinger: List<HistoriskUtbetaling>) {

    internal fun results() : List<Utbetalingstidslinje> {
        return historiskUtbetalinger.mapNotNull { it.toTidslinje(fom, tom) }
    }

    internal fun maksdatoer() : List<LocalDate> {
        return historiskUtbetalinger.mapNotNull { it.maksdato() }
    }

}
