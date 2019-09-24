package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.streams.toList

internal class CompositeInterval(
    interval: List<Interval?>
) : Interval() {

    private val interval = interval.filterNotNull()

    override fun rapportertDato() = interval.maxBy { it.rapportertDato() }!!.rapportertDato()

    override fun flatten(): List<Dag> {
        return interval.flatMap { it.flatten() }
    }

    companion object {
        fun syk(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): CompositeInterval {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeInterval(fra.datesUntil(til.plusDays(1)).map {
                Sykedag(
                    it,
                    rapportert
                )
            }.toList())
        }

        fun ikkeSyk(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): CompositeInterval {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeInterval(fra.datesUntil(til.plusDays(1)).map {
                ikkeSykedag(
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
