package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

class Feriedag(private val dagFeriedagenDekker: LocalDate, private val tidspunktRapportert: LocalDateTime) {
    fun startdato() = dagFeriedagenDekker
    fun sluttdato() = dagFeriedagenDekker
    fun antallSykedager() = 0
}
