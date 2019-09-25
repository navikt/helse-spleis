package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

internal class SykHelgedag internal constructor(gjelder: LocalDate, rapportert: LocalDateTime): Dag(gjelder, rapportert, 10) {
    override fun antallSykedager() = 1
}
