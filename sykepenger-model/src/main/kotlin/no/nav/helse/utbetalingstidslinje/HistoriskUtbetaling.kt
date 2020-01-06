package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.dag.erHelg
import java.time.LocalDate

internal class HistoriskUtbetaling(private val fom: LocalDate, private val tom: LocalDate, private val orgnummer: Int) {

    internal fun toTidslinje(rangeFom: LocalDate, rangeTom: LocalDate) = Utbetalingstidslinje().apply {
        maxOf(rangeFom, fom).datesUntil(minOf(rangeTom, tom).plusDays(1)).forEach {
            if (it.erHelg()) this.addHelg(0.0, it) else this.addNAVdag(0.0, it)
        }
    }
}
