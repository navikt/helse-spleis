package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

abstract class Sykdomstidslinje {
    abstract fun startdato(): LocalDate
    abstract fun sluttdato(): LocalDate
    abstract fun antallSykedagerHvorViTellerMedHelg(): Int
    abstract fun antallSykedagerHvorViIkkeTellerMedHelg(): Int
    abstract fun flatten(): List<Dag>
    abstract fun length(): Int
    abstract fun accept(visitor: SykdomstidslinjeVisitor)
    internal abstract fun jsonRepresentation(): List<JsonDag>
    internal abstract fun sisteHendelse(): Sykdomshendelse
    internal abstract fun dag(dato: LocalDate, hendelse: Sykdomshendelse): Dag
    fun toJson(): String = objectMapper.writeValueAsString(jsonRepresentation())

    operator fun plus(other: Sykdomstidslinje): Sykdomstidslinje {
        if (this.startdato().isAfter(other.startdato())) return other + this

        val datesUntil = this.førsteStartdato(other).datesUntil(this.sisteSluttdato(other).plusDays(1)).toList()
        val intervalEtterKonflikter =
            datesUntil.map { this.dag(it, this.sisteHendelse()).beste(other.dag(it, other.sisteHendelse())) }.toList()

        return CompositeSykdomstidslinje(intervalEtterKonflikter.map { it.tilDag() })
    }

    internal fun antallDagerMellom(other: Sykdomstidslinje) =
        when {
            inneholder(other) -> -min(this.length(), other.length())
            harOverlapp(other) -> max(this.avstandMedOverlapp(other), other.avstandMedOverlapp(this))
            else -> min(this.avstand(other), other.avstand(this))
        }

    fun overlapperMed(other: Sykdomstidslinje) =
        this.antallDagerMellom(other) < 0

    private fun førsteStartdato(other: Sykdomstidslinje) =
        if (this.startdato().isBefore(other.startdato())) this.startdato() else other.startdato()

    private fun sisteSluttdato(other: Sykdomstidslinje) =
        if (this.sluttdato().isAfter(other.sluttdato())) this.sluttdato() else other.sluttdato()

    private fun avstand(other: Sykdomstidslinje) =
        this.sluttdato().until(other.startdato(), ChronoUnit.DAYS).absoluteValue.toInt() - 1

    private fun avstandMedOverlapp(other: Sykdomstidslinje) =
        -(this.sluttdato().until(other.startdato(), ChronoUnit.DAYS).absoluteValue.toInt() + 1)

    private fun erDelAv(other: Sykdomstidslinje) =
        this.harBeggeGrenseneInnenfor(other) || other.harBeggeGrenseneInnenfor(this)

    private fun inneholder(other: Sykdomstidslinje) =
        this.harBeggeGrenseneInnenfor(other) || other.harBeggeGrenseneInnenfor(this)

    private fun harBeggeGrenseneInnenfor(other: Sykdomstidslinje) =
        this.startdato() in other.startdato()..other.sluttdato() && this.sluttdato() in other.startdato()..other.sluttdato()

    private fun harOverlapp(other: Sykdomstidslinje) = this.harGrenseInnenfor(other) || other.harGrenseInnenfor(this)

    private fun harGrenseInnenfor(other: Sykdomstidslinje) =
        this.startdato() in (other.startdato()..other.sluttdato())

    internal fun trim(): Sykdomstidslinje {
        val days = flatten()
            .dropWhile { it.antallSykedagerHvorViTellerMedHelg() < 1 }
            .dropLastWhile { it.antallSykedagerHvorViTellerMedHelg() < 1 }
        return CompositeSykdomstidslinje(days)
    }

    fun syketilfeller(): List<Sykdomstidslinje> {
        val visitor = SyketilfelleSplitter()
        this.accept(visitor)

        return visitor.results()
    }

    fun utbetalingstidslinje(dagsats: Double) = Utbetalingstidslinje(dagsats).also { this.accept(it) }

    companion object {
        fun sykedag(gjelder: LocalDate, hendelse: Sykdomshendelse) =
            if (erArbeidsdag(gjelder)) Sykedag(
                gjelder,
                hendelse
            ) else SykHelgedag(
                gjelder,
                hendelse
            )

        fun egenmeldingsdag(gjelder: LocalDate, hendelse: Sykdomshendelse) =
            Egenmeldingsdag(gjelder, hendelse)

        fun ferie(gjelder: LocalDate, hendelse: Sykdomshendelse) =
            if (erArbeidsdag(gjelder)) Feriedag(
                gjelder,
                hendelse
            ) else Helgedag(
                gjelder,
                hendelse
            )

        fun ikkeSykedag(gjelder: LocalDate, hendelse: Sykdomshendelse) =
            if (erArbeidsdag(gjelder)) Arbeidsdag(
                gjelder,
                hendelse
            ) else Helgedag(
                gjelder,
                hendelse
            )

        fun utenlandsdag(gjelder: LocalDate, hendelse: Sykdomshendelse) =
            if (erArbeidsdag(gjelder)) Utenlandsdag(
                gjelder,
                hendelse
            ) else Helgedag(
                gjelder,
                hendelse
            )

        fun sykedager(fra: LocalDate, til: LocalDate, hendelse: Sykdomshendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                sykedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun egenmeldingsdager(fra: LocalDate, til: LocalDate, hendelse: Sykdomshendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                egenmeldingsdag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun ferie(fra: LocalDate, til: LocalDate, hendelse: Sykdomshendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ferie(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun ikkeSykedager(fra: LocalDate, til: LocalDate, hendelse: Sykdomshendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ikkeSykedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun utenlandsdager(fra: LocalDate, til: LocalDate, hendelse: Sykdomshendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                utenlandsdag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun studiedag(gjelder: LocalDate, hendelse: Sykdomshendelse) =
            if (erArbeidsdag(gjelder)) Studiedag(
                gjelder,
                hendelse
            ) else Helgedag(
                gjelder,
                hendelse
            )

        fun studiedager(fra: LocalDate, til: LocalDate, hendelse: Sykdomshendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                studiedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun permisjonsdag(gjelder: LocalDate, hendelse: Sykdomshendelse) =
            if (erArbeidsdag(gjelder)) Permisjonsdag(
                gjelder,
                hendelse
            ) else Helgedag(
                gjelder,
                hendelse
            )

        fun permisjonsdager(fra: LocalDate, til: LocalDate, hendelse: Sykdomshendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                permisjonsdag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun fromJson(json: String): Sykdomstidslinje {
            return CompositeSykdomstidslinje.fromJsonRepresentation(objectMapper.readValue(json))
        }

        private fun erArbeidsdag(dato: LocalDate) =
            dato.dayOfWeek != DayOfWeek.SATURDAY && dato.dayOfWeek != DayOfWeek.SUNDAY
    }
}
