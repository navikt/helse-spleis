package no.nav.helse.util.interval

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.streams.toList

abstract class Sykdomstidslinje {
    abstract fun startdato(): LocalDate
    abstract fun sluttdato(): LocalDate
    abstract fun antallSykedager(): Int
    abstract fun flatten(): List<Dag>

    internal abstract fun dag(dato: LocalDate): Dag
    internal abstract fun rapportertDato(): LocalDateTime

    operator fun plus(other: Sykdomstidslinje): Sykdomstidslinje {
        if (this.startdato().isAfter(other.startdato())) return other + this

        val datesUntil = førsteStartdato(other).datesUntil(sisteSluttdato(other).plusDays(1)).toList()
        val intervalEtterKonflikter =
            datesUntil.map { this.beste(other, it) }.toList()

        return CompositeSykdomstidslinje(intervalEtterKonflikter.map { it.tilDag() })
    }

    private fun beste(other: Sykdomstidslinje, dato: LocalDate) = listOf(this.dag(dato), other.dag(dato)).max()!!

    private fun harGrenseInnenfor(other: Sykdomstidslinje) =
        this.startdato() in (other.startdato()..other.sluttdato())

    private fun førsteStartdato(other: Sykdomstidslinje) =
        if (this.startdato().isBefore(other.startdato())) this.startdato() else other.startdato()

    private fun sisteSluttdato(other: Sykdomstidslinje) =
        if (this.sluttdato().isAfter(other.sluttdato())) this.sluttdato() else other.sluttdato()

    companion object {
        fun sykedager(gjelder: LocalDate, rapportert: LocalDateTime) =
            if (erArbeidsdag(gjelder)) Sykedag(gjelder, rapportert) else SykHelgedag(gjelder, rapportert)

        fun ferie(gjelder: LocalDate, rapportert: LocalDateTime) =
            if (erArbeidsdag(gjelder)) Feriedag(gjelder, rapportert) else Helgedag(gjelder, rapportert)

        fun ikkeSykedag(gjelder: LocalDate, rapportert: LocalDateTime) =
            if (erArbeidsdag(gjelder)) Arbeidsdag(gjelder, rapportert) else Helgedag(gjelder, rapportert)

        fun sykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                sykedager(
                    it,
                    rapportert
                )
            }.toList())
        }

        fun ferie(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ferie(
                    it,
                    rapportert
                )
            }.toList())
        }

        fun ikkeSykedager(fra: LocalDate, til: LocalDate, rapportert: LocalDateTime): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ikkeSykedag(
                    it,
                    rapportert
                )
            }.toList())
        }

        private fun erArbeidsdag(dato: LocalDate) =
            dato.dayOfWeek != DayOfWeek.SATURDAY && dato.dayOfWeek != DayOfWeek.SUNDAY

    }
}
