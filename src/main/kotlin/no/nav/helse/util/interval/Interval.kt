package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

interface Interval {
    fun startdato(): LocalDate
    fun sluttdato(): LocalDate
    fun antallSykedager(): Int

    companion object {
        fun sykedager(gjelder: LocalDate, rapportert: LocalDateTime) = Sykedag(gjelder, rapportert)
        fun helgedag(gjelder: LocalDate, rapportert: LocalDateTime) = Helgedag(gjelder, rapportert)
        fun feriedag(gjelder: LocalDate, rapportert: LocalDateTime) = Feriedag(gjelder, rapportert)
        fun arbeidsdag(gjelder: LocalDate, rapportert: LocalDateTime) = Arbeidsdag(gjelder, rapportert)
    }

//    operator fun plus(other: Interval)
}
