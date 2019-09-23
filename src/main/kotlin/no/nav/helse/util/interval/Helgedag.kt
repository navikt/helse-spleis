package no.nav.helse.util.interval

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class Helgedag(gjelder: LocalDate, rapportert: LocalDateTime): Dag(gjelder, rapportert){

    init {
        require(gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY) {"En helgedag må være lørdag eller søndag"}
    }

    override fun antallSykedager() = 0
}
