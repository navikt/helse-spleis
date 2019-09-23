package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

class Feriedag(private val dagFeriedagenDekker: LocalDate, private val tidspunktRapportert: LocalDateTime) : Interval {
    override fun startdato() = dagFeriedagenDekker
    override fun sluttdato() = dagFeriedagenDekker
    override fun antallSykedager() = 0
}
