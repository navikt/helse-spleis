package no.nav.helse.util.interval

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class Helgedag(private val helgedagenGjelder: LocalDate, private val tidspunktRapportert: LocalDateTime): Interval {

    init {
        require(helgedagenGjelder.dayOfWeek == DayOfWeek.SATURDAY || helgedagenGjelder.dayOfWeek == DayOfWeek.SUNDAY) {"En helgedag må være lørdag eller søndag"}
    }

    override fun startdato() = helgedagenGjelder
    override fun sluttdato() = helgedagenGjelder
    override fun antallSykedager() = 0
}
