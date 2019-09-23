package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

class Arbeidsdag internal constructor(gjelder: LocalDate, rapportert: LocalDateTime): Dag(gjelder, rapportert) {
    override fun antallSykedager() = 0
}
