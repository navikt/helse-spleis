package no.nav.helse.sykdomstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

internal class Helgedag internal constructor(gjelder: LocalDate, rapportert: LocalDateTime): Dag(gjelder, rapportert, 20){

    init {
        require(gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY) {"En ikkeSykedag må være lørdag eller søndag"}
    }

    override fun antallSykedager() = 0

    override fun antallSykeVirkedager() = 0

    override fun toString() = formatter.format(dagen) + "\tHelg"
}
