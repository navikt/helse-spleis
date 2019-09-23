package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

interface Interval {
    fun startdato(): LocalDate
    fun sluttdato(): LocalDate
    fun antallSykedager(): Int

    companion object {
        fun sykedager(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Sykedag(gjelder, rapportert)
        fun sykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Interval =
            SimpleCompositeInterval.syk(fra, til, rapportert)

        fun helgedag(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Helgedag(gjelder, rapportert)
        fun feriedag(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Feriedag(gjelder, rapportert)
        fun arbeidsdag(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Arbeidsdag(gjelder, rapportert)
    }

//    operator fun plus(other: Interval)
}
