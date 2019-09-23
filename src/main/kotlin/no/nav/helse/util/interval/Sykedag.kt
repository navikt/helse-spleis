package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

class Sykedag(private val dagSykedagenDekker: LocalDate, private val tidspunktRapportert: LocalDateTime) {
    fun startdato() = dagSykedagenDekker
    fun sluttdato() = dagSykedagenDekker
    fun antallSykedager() = 1
}
