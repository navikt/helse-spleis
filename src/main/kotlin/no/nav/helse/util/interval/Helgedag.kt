package no.nav.helse.util.interval

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

internal class Helgedag internal constructor(gjelder: LocalDate, rapportert: LocalDateTime): Dag(gjelder, rapportert){

    init {
        require(gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY) {"En ikkeSykedag må være lørdag eller søndag"}
    }

    override fun antallSykedager() = 0
}
