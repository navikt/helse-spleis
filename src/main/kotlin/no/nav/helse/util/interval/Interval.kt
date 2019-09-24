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
        if (this.startdato().isAfter(other.startdato())) return other + this
        return CompositeInterval(listOf(this, gap(this, other), other))
    }

    fun pluss(other: Interval) = this + other
    private fun harOverlapp(other: Interval) = this.harGrenseInnenfor(other) || other.harGrenseInnenfor(this)
    internal abstract fun rapportertDato(): LocalDateTime

    private fun gap(start: Interval, slutt: Interval): Interval? {
        if (start.sluttdato().plusDays(1) >= slutt.startdato()) return null
        val rapportertDato = listOf(start, slutt).maxBy { it.rapportertDato() }!!.rapportertDato()
        return ikkeSykedager(start.sluttdato().plusDays(1), slutt.startdato().minusDays(1), rapportertDato)
    }

    private fun harGrenseInnenfor(other: Interval) =
        this.startdato() in (other.startdato()..other.sluttdato())


    companion object {
        fun sykedager(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Sykedag(gjelder, rapportert)

        fun sykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Interval =
            CompositeInterval.syk(fra, til, rapportert)

        fun ikkeSykedag(gjelder: LocalDate, rapportert: LocalDateTime): Interval {
            return if (gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY)
                Helgedag(gjelder, rapportert) else Arbeidsdag(gjelder, rapportert)
        }

        fun ikkeSykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Interval =
            CompositeInterval.ikkeSyk(fra, til, rapportert)

        fun feriedag(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Feriedag(gjelder, rapportert)
    }
}
