package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.tournament.Dagturnering
import java.time.LocalDate
import kotlin.streams.toList

internal interface SykdomstidslinjeElement {
    fun accept(visitor: SykdomstidslinjeVisitor)
}

internal abstract class ConcreteSykdomstidslinje : SykdomstidslinjeElement {
    abstract fun førsteDag(): LocalDate
    abstract fun sisteDag(): LocalDate
    internal abstract fun flatten(): List<Dag>
    internal abstract fun length(): Int
    internal abstract fun dag(dato: LocalDate): Dag?

    fun plus(
        other: ConcreteSykdomstidslinje,
        gapDayCreator: (LocalDate, Dag.Kildehendelse) -> Dag,
        dagturnering: Dagturnering
    ): ConcreteSykdomstidslinje {
        if (this.length() == 0) return other
        if (other.length() == 0) return this

        val førsteStartdato = this.førsteStartdato(other)

        return CompositeSykdomstidslinje(
            førsteStartdato.datesUntil(this.sisteSluttdato(other).plusDays(1))
                .map {
                    val firstOfOther = other.dag(other.førsteDag())
                    beste(this.dag(it), other.dag(it), dagturnering) ?: gapDayCreator(it, firstOfOther!!.kildehendelse)
                }.toList()
        )
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

    @Deprecated("Skal bruke Aktivitetslogger.error()")
    fun erUtenforOmfang(): Boolean {
        return flatten().any { it::class in arrayOf(Permisjonsdag::class, Ubestemtdag::class) }
    }

    private fun førsteStartdato(other: ConcreteSykdomstidslinje) =
        if (this.førsteDag().isBefore(other.førsteDag())) this.førsteDag() else other.førsteDag()

    private fun sisteSluttdato(other: ConcreteSykdomstidslinje) =
        if (this.sisteDag().isAfter(other.sisteDag())) this.sisteDag() else other.sisteDag()

    private fun harGrenseInnenfor(other: ConcreteSykdomstidslinje) =
        this.førsteDag() in (other.førsteDag()..other.sisteDag())

    internal fun harTilstøtende(other: ConcreteSykdomstidslinje) = this.sisteDag().harTilstøtende(other.førsteDag())

    companion object {
        fun sykedag(gjelder: LocalDate, hendelseType: Dag.Kildehendelse) =
            if (!gjelder.erHelg()) Sykedag(gjelder, hendelseType) else SykHelgedag(gjelder, hendelseType)

        fun egenmeldingsdag(gjelder: LocalDate, hendelseType: Dag.Kildehendelse) =
            Egenmeldingsdag(gjelder, hendelseType)

        fun ferie(gjelder: LocalDate, hendelseType: Dag.Kildehendelse) =
            Feriedag(gjelder, hendelseType)

        fun ikkeSykedag(gjelder: LocalDate, hendelseType: Dag.Kildehendelse) =
            if (!gjelder.erHelg()) Arbeidsdag(gjelder, hendelseType) else ImplisittDag(gjelder, hendelseType)

        fun utenlandsdag(gjelder: LocalDate, hendelseType: Dag.Kildehendelse) =
            if (!gjelder.erHelg()) Utenlandsdag(gjelder, hendelseType) else ImplisittDag(gjelder, hendelseType)

        fun sykedager(fra: LocalDate, til: LocalDate, hendelseType: Dag.Kildehendelse): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                sykedag(
                    it, hendelseType
                )
            }.toList())
        }

        fun egenmeldingsdager(
            fra: LocalDate,
            til: LocalDate,
            hendelseType: Dag.Kildehendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1))
                .map { egenmeldingsdag(it, hendelseType) }
                .toList())
        }

        fun ferie(fra: LocalDate, til: LocalDate, hendelseType: Dag.Kildehendelse): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1))
                .map { ferie(it, hendelseType) }
                .toList())
        }

        fun ikkeSykedager(
            fra: LocalDate,
            til: LocalDate,
            hendelseType: Dag.Kildehendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1))
                .map { ikkeSykedag(it, hendelseType) }
                .toList())
        }

        fun utenlandsdager(
            fra: LocalDate,
            til: LocalDate,
            hendelseType: Dag.Kildehendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                utenlandsdag(
                    it, hendelseType
                )
            }.toList())
        }

        fun studiedag(gjelder: LocalDate, hendelseType: Dag.Kildehendelse) =
            if (!gjelder.erHelg()) Studiedag(
                gjelder, hendelseType
            ) else ImplisittDag(
                gjelder, hendelseType
            )

        fun studiedager(
            fra: LocalDate,
            til: LocalDate,
            hendelseType: Dag.Kildehendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                studiedag(
                    it, hendelseType
                )
            }.toList())
        }

        fun permisjonsdag(gjelder: LocalDate, hendelseType: Dag.Kildehendelse) =
            if (!gjelder.erHelg()) Permisjonsdag(
                gjelder, hendelseType
            ) else ImplisittDag(
                gjelder, hendelseType
            )

        internal fun implisittDag(gjelder: LocalDate, hendelseType: Dag.Kildehendelse) =
            if (!gjelder.erHelg()) ImplisittDag(
                gjelder, hendelseType
            ) else ImplisittDag(
                gjelder, hendelseType
            )

        fun permisjonsdager(
            fra: LocalDate,
            til: LocalDate,
            hendelseType: Dag.Kildehendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { permisjonsdag(it, hendelseType) }
                    .toList())
        }

        fun implisittdager(
            fra: LocalDate,
            til: LocalDate,
            hendelseType: Dag.Kildehendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { implisittDag(it, hendelseType) }
                    .toList())
        }

        fun ubestemtdager(
            fra: LocalDate,
            til: LocalDate,
            hendelseType: Dag.Kildehendelse
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { Ubestemtdag(it, hendelseType) }
                    .toList())
        }

        private fun beste(a: Dag?, b: Dag?, dagturnering: Dagturnering): Dag? {
            if (a == null) return b
            if (b == null) return a
            return a.beste(b, dagturnering)
        }
    }
}
