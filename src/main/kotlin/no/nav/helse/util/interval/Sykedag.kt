package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

internal class Sykedag internal constructor(gjelder: LocalDate, rapportert: LocalDateTime) : Dag(gjelder, rapportert, 10) {
//    override fun plus(other: Interval) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

    override fun antallSykedager() = 1
}
