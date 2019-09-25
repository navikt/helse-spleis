package no.nav.helse.util.interval

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.streams.toList

abstract class Interval {
    abstract fun startdato(): LocalDate
    abstract fun sluttdato(): LocalDate
    abstract fun antallSykedager(): Int
    abstract fun flatten(): List<Dag>
    operator fun plus(other: Interval): Interval {
//        førsteStartdato(other).datesUntil(sisteSluttdato(other)).map { this.beste(other, it)}

        if (this.startdato().isAfter(other.startdato())) return other + this
        return CompositeInterval(listOf(this, gap(this, other), other))
    }

    private fun beste(other: Interval, dato: LocalDate): Interval {
        return listOf(this.dag(dato), other.dag(dato)).max()!!
    }

    internal abstract fun dag(dato: LocalDate): Dag

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

    private fun førsteStartdato(other: Interval) =
        if (this.startdato().isBefore(other.startdato())) this.startdato() else other.startdato()

    private fun sisteSluttdato(other: Interval) =
        if (this.sluttdato().isAfter(other.sluttdato())) this.sluttdato() else other.sluttdato()

    companion object {
        fun sykedager(gjelder: LocalDate, rapportert: LocalDateTime): Dag = Sykedag(gjelder, rapportert)

        fun sykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Interval {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeInterval(fra.datesUntil(til.plusDays(1)).map {
                Sykedag(
                    it,
                    rapportert
                )
            }.toList())
        }

        fun ferie(gjelder: LocalDate, rapportert: LocalDateTime): Dag {
            return if (gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY)
                Helgedag(gjelder, rapportert) else Feriedag(gjelder, rapportert)
        }

        fun ikkeSykedag(gjelder: LocalDate, rapportert: LocalDateTime): Dag {
            return if (gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY)
                Helgedag(gjelder, rapportert) else Arbeidsdag(gjelder, rapportert)
        }

        fun ikkeSykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Interval {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeInterval(fra.datesUntil(til.plusDays(1)).map {
                ikkeSykedag(
                    it,
                    rapportert
                )
            }.toList())
        }

        fun ferie(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Interval {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeInterval(fra.datesUntil(til.plusDays(1)).map {
                ferie(
                    it,
                    rapportert
                )
            }.toList())
        }

        fun feriedag(gjelder: LocalDate, rapportert: LocalDateTime): Interval = Feriedag(gjelder, rapportert)

    }
}
