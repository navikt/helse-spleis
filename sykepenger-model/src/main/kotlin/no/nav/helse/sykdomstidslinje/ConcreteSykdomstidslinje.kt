package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.sykdomstidslinje.dag.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

private val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

internal interface SykdomstidslinjeElement {
    fun accept(visitor: SykdomstidslinjeVisitor)
}

internal abstract class ConcreteSykdomstidslinje : SykdomstidslinjeElement {
    abstract fun førsteDag(): LocalDate
    abstract fun sisteDag(): LocalDate
    abstract fun hendelser(): Set<SykdomstidslinjeHendelse>
    internal abstract fun flatten(): List<Dag>

    internal abstract fun length(): Int
    internal abstract fun sisteHendelse(): SykdomstidslinjeHendelse

    internal abstract fun dag(dato: LocalDate): Dag?

    fun plus(
        other: ConcreteSykdomstidslinje,
        gapDayCreator: (LocalDate, SykdomstidslinjeHendelse) -> Dag
    ): ConcreteSykdomstidslinje {
        if (this.length() == 0) return other
        if (other.length() == 0) return this

        if (this.førsteDag().isAfter(other.førsteDag())) return other.plus(this, gapDayCreator)

        return CompositeSykdomstidslinje(
            this.førsteDag().datesUntil(this.sisteSluttdato(other).plusDays(1))
                .map {
                    beste(this.dag(it), other.dag(it)) ?: gapDayCreator(it, other.sisteHendelse())
                }.toList()
        )
    }

    operator fun plus(other: ConcreteSykdomstidslinje): ConcreteSykdomstidslinje {
        return this.plus(other, Companion::implisittDag)
    }

    internal fun kutt(kuttDag: LocalDate): ConcreteSykdomstidslinje? {
        if (kuttDag.isBefore(førsteDag())) return null
        if (!kuttDag.isBefore(sisteDag())) return this
        return CompositeSykdomstidslinje(this.flatten().filterNot { it.dagen.isAfter(kuttDag) })
    }

    fun overlapperMed(other: ConcreteSykdomstidslinje) =
        when {
            this.length() == 0 || other.length() == 0 -> false
            else -> this.harGrenseInnenfor(other) || other.harGrenseInnenfor(this)
        }

    fun erUtenforOmfang(): Boolean {
        return flatten().any { it::class in arrayOf(Permisjonsdag::class, Ubestemtdag::class) }
    }

    internal fun antallDagerMellom(other: ConcreteSykdomstidslinje) =
        when {
            this.length() == 0 || other.length() == 0 -> throw IllegalStateException("Kan ikke regne antall dager mellom tidslinjer, når én eller begge er tomme.")
            erDelAv(other) -> -min(this.length(), other.length())
            overlapperMed(other) -> max(this.avstandMedOverlapp(other), other.avstandMedOverlapp(this))
            else -> min(this.avstand(other), other.avstand(this))
        }

    private fun førsteStartdato(other: ConcreteSykdomstidslinje) =
        if (this.førsteDag().isBefore(other.førsteDag())) this.førsteDag() else other.førsteDag()

    private fun sisteSluttdato(other: ConcreteSykdomstidslinje) =
        if (this.sisteDag().isAfter(other.sisteDag())) this.sisteDag() else other.sisteDag()

    private fun avstand(other: ConcreteSykdomstidslinje) =
        this.sisteDag().until(other.førsteDag(), ChronoUnit.DAYS).absoluteValue.toInt() - 1

    private fun avstandMedOverlapp(other: ConcreteSykdomstidslinje) =
        -(this.sisteDag().until(other.førsteDag(), ChronoUnit.DAYS).absoluteValue.toInt() + 1)

    private fun erDelAv(other: ConcreteSykdomstidslinje) =
        this.harBeggeGrenseneInnenfor(other) || other.harBeggeGrenseneInnenfor(this)

    private fun harBeggeGrenseneInnenfor(other: ConcreteSykdomstidslinje) =
        this.førsteDag() in other.førsteDag()..other.sisteDag() && this.sisteDag() in other.førsteDag()..other.sisteDag()

    private fun harGrenseInnenfor(other: ConcreteSykdomstidslinje) =
        this.førsteDag() in (other.førsteDag()..other.sisteDag())


    private fun jsonRepresentation(): JsonTidslinje {
        val dager = flatten().map { it.toJsonDag() }
        val hendelser = flatten().flatMap { it.toJsonHendelse() }.distinctBy { it.hendelseId() }
            .map { objectMapper.readTree(it.toJson()) }
        return JsonTidslinje(dager = dager, hendelser = hendelser)
    }

    companion object {

        fun sykedag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (!gjelder.erHelg()) Sykedag(
                gjelder,
                hendelse
            ) else SykHelgedag(
                gjelder,
                hendelse
            )

        fun egenmeldingsdag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            Egenmeldingsdag(gjelder, hendelse)

        fun ferie(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            Feriedag(
                gjelder,
                hendelse
            )

        fun ikkeSykedag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (!gjelder.erHelg()) Arbeidsdag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        fun utenlandsdag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (!gjelder.erHelg()) Utenlandsdag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        fun sykedager(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                sykedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun egenmeldingsdager(
            fra: LocalDate,
            til: LocalDate,
            hendelse: SykdomstidslinjeHendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                egenmeldingsdag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun ferie(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ferie(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun ikkeSykedager(
            fra: LocalDate,
            til: LocalDate,
            hendelse: SykdomstidslinjeHendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ikkeSykedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun utenlandsdager(
            fra: LocalDate,
            til: LocalDate,
            hendelse: SykdomstidslinjeHendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                utenlandsdag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun studiedag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (!gjelder.erHelg()) Studiedag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        fun studiedager(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                studiedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun permisjonsdag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (!gjelder.erHelg()) Permisjonsdag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        internal fun implisittDag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (!gjelder.erHelg()) ImplisittDag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        fun permisjonsdager(
            fra: LocalDate,
            til: LocalDate,
            hendelse: SykdomstidslinjeHendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { permisjonsdag(it, hendelse) }
                    .toList())
        }

        fun implisittdager(
            fra: LocalDate,
            til: LocalDate,
            hendelse: SykdomstidslinjeHendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { implisittDag(it, hendelse) }
                    .toList())
        }

        fun ubestemtdager(
            fra: LocalDate,
            til: LocalDate,
            hendelse: SykdomstidslinjeHendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { Ubestemtdag(it, hendelse) }
                    .toList())
        }

        private fun beste(a: Dag?, b: Dag?): Dag? {
            if (a == null) return b
            if (b == null) return a
            return a.beste(b)
        }
    }
}
