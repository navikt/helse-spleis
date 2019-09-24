package no.nav.helse.util.interval

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

abstract class Interval {
    abstract fun startdato(): LocalDate
    abstract fun sluttdato(): LocalDate
    abstract fun antallSykedager(): Int
    abstract fun flatten(): List<Dag>
    operator fun plus(other: Interval): Interval {
        return SimpleCompositeInterval(listOf(this, other).sortedBy { it.startdato() })
    }

    fun pluss(other: Interval) = this + other
    internal fun harOverlapp(other: Interval) = this.harGrenseInnenfor(other) || other.harGrenseInnenfor(this)

    private fun harGrenseInnenfor(other: Interval) =
        this.startdato() in (other.startdato()..other.sluttdato())


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
}
