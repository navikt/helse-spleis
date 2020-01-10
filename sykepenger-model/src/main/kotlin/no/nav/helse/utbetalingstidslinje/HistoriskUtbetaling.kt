package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.dag.erHelg
import java.time.LocalDate

internal class HistoriskUtbetaling(
    private val orgnummer: Int,
    private val fom: LocalDate,
    private val tom: LocalDate
) {

    companion object {
        internal fun finnSisteNavDagFor(betalinger: List<HistoriskUtbetaling>, orgnummer: Int, before: LocalDate) : LocalDate? {
            return betalinger
                .filter { it.orgnummer == orgnummer }
                .map { it.tom }
                .filter { it < before }
                .max()
        }

        internal fun utbetalingstidslinje(betalinger: List<HistoriskUtbetaling>) =
            betalinger.mapNotNull { it.toTidslinje() }.reduce(Utbetalingstidslinje::plus)
    }

    internal fun toTidslinje(rangeFom: LocalDate = fom, rangeTom: LocalDate = tom): Utbetalingstidslinje? {
        if (fom > rangeTom || tom < rangeFom) return null
        return Utbetalingstidslinje().apply {
            maxOf(rangeFom, fom).datesUntil(minOf(rangeTom, tom).plusDays(1)).forEach {
                if (it.erHelg()) this.addHelg(0.0, it) else this.addNAVdag(0.0, it)
            }
        }
    }

}

internal fun List<HistoriskUtbetaling>.finnSisteNavDagFor(orgnummer: Int, before: LocalDate) =
    HistoriskUtbetaling.finnSisteNavDagFor(this, orgnummer, before)
