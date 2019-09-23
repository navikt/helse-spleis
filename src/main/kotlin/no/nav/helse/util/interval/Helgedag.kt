package no.nav.helse.util.interval

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class Helgedag(private val helgedagenGjelder: LocalDate, private val tidspunktRapportert: LocalDateTime) {

    init {
        require(helgedagenGjelder.dayOfWeek == DayOfWeek.SATURDAY || helgedagenGjelder.dayOfWeek == DayOfWeek.SUNDAY) {"En helgedag må være lørdag eller søndag"}
    }

    fun startdato() = helgedagenGjelder
    fun sluttdato() = helgedagenGjelder
    fun antallSykedager() = 0
}
