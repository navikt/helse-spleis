package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.streams.toList

internal class SimpleCompositeInterval(
    private val interval: List<Interval>
) : Interval {

    companion object {
        fun syk(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): SimpleCompositeInterval {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return SimpleCompositeInterval(fra.datesUntil(til.plusDays(1)).map {
                Sykedag(
                    it,
                    rapportert
                )
            }.toList())
        }
    }

    override fun startdato() = interval.first().startdato()

    override fun sluttdato() = interval.last().sluttdato()

    override fun antallSykedager() = interval.sumBy { it.antallSykedager() }

}
