package no.nav.helse.util.interval

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

interface Interval {
    fun startdato(): LocalDate
    fun sluttdato(): LocalDate
    fun antallSykedager(): Int
    fun flatten(): List<Dag>

    companion object {
        fun sykedager(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Sykedag(gjelder, rapportert)

        fun sykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Interval =
            SimpleCompositeInterval.syk(fra, til, rapportert)

        fun ikkeSykedag(gjelder: LocalDate, rapportert: LocalDateTime): Interval {
            return if (gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY)
                Helgedag(gjelder, rapportert) else Arbeidsdag(gjelder, rapportert)
        }

        fun ikkeSykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Interval =
            SimpleCompositeInterval.ikkeSyk(fra, til, rapportert)

        fun feriedag(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Feriedag(gjelder, rapportert)
    }

//    operator fun plus(other: Interval)
}
