package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

class Arbeidsdag(private val arbeidsdagenGjelder: LocalDate, private val tidspunktRapportert: LocalDateTime) {
    fun startdato() = arbeidsdagenGjelder
    fun sluttdato() = arbeidsdagenGjelder
    fun antallSykedager() = 0
}
