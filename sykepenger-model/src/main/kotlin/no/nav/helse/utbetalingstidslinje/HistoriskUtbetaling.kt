package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.dag.erHelg
import java.time.LocalDate

internal class HistoriskUtbetaling(
    private val orgnummer: Int,
    private val fom: LocalDate,
    private val tom: LocalDate
) {

    internal fun toTidslinje(rangeFom: LocalDate, rangeTom: LocalDate): Utbetalingstidslinje? {
        if (fom > rangeTom || tom < rangeFom) return null
        return Utbetalingstidslinje().apply {
            maxOf(rangeFom, fom).datesUntil(minOf(rangeTom, tom).plusDays(1)).forEach {
                if (it.erHelg()) this.addHelg(0.0, it) else this.addNAVdag(0.0, it)
            }
        }
    }

}
