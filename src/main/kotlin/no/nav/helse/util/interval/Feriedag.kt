package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

internal class Feriedag internal constructor(gjelder: LocalDate, rapportert: LocalDateTime) : Dag(gjelder, rapportert, 20) {
    override fun antallSykedager() = 0
}
