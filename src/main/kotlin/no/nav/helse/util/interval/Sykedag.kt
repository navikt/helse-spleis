package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

class Sykedag(private val dagSykedagenDekker: LocalDate, private val tidspunktRapportert: LocalDateTime) : Interval {
    override fun startdato() = dagSykedagenDekker
    override fun sluttdato() = dagSykedagenDekker
    override fun antallSykedager() = 1
}
