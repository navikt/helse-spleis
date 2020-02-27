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
        gapDayCreator: (LocalDate) -> Dag,
        dagturnering: Dagturnering
    ): ConcreteSykdomstidslinje {
        if (this.length() == 0) return other
        if (other.length() == 0) return this

        val førsteStartdato = this.førsteStartdato(other)

        return CompositeSykdomstidslinje(
            førsteStartdato.datesUntil(this.sisteSluttdato(other).plusDays(1))
                .map {
                    val firstOfOther = other.dag(other.førsteDag())
                    beste(this.dag(it), other.dag(it), dagturnering) ?: gapDayCreator(it)
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
        return flatten().any { it::class in arrayOf(Permisjonsdag.Søknad::class, Permisjonsdag.Aareg::class, Ubestemtdag::class) }
    }

    private fun førsteStartdato(other: ConcreteSykdomstidslinje) =
        if (this.førsteDag().isBefore(other.førsteDag())) this.førsteDag() else other.førsteDag()

    private fun sisteSluttdato(other: ConcreteSykdomstidslinje) =
        if (this.sisteDag().isAfter(other.sisteDag())) this.sisteDag() else other.sisteDag()

    private fun harGrenseInnenfor(other: ConcreteSykdomstidslinje) =
        this.førsteDag() in (other.førsteDag()..other.sisteDag())

    internal fun harTilstøtende(other: ConcreteSykdomstidslinje) = this.sisteDag().harTilstøtende(other.førsteDag())

    companion object {
        fun sykedag(gjelder: LocalDate, grad: Double, factory: DagFactory): Dag =
            if (!gjelder.erHelg()) factory.sykedag(gjelder, grad) else factory.sykHelgedag(gjelder, grad)

        fun egenmeldingsdag(gjelder: LocalDate, factory: DagFactory) =
            factory.egenmeldingsdag(gjelder)

        fun ferie(gjelder: LocalDate, factory: DagFactory) =
            factory.feriedag(gjelder)

        fun ikkeSykedag(gjelder: LocalDate, factory: DagFactory) =
            if (!gjelder.erHelg()) factory.arbeidsdag(gjelder) else factory.implisittDag(gjelder)

        fun utenlandsdag(gjelder: LocalDate, factory: DagFactory) =
            if (!gjelder.erHelg()) factory.utenlandsdag(gjelder) else factory.implisittDag(gjelder)

        fun sykedager(fra: LocalDate, til: LocalDate, grad: Double, factory: DagFactory): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                sykedag(
                    it, grad, factory
                )
            }.toList())
        }

        fun egenmeldingsdager(
            fra: LocalDate,
            til: LocalDate,
            factory: DagFactory
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1))
                .map { egenmeldingsdag(it, factory) }
                .toList())
        }

        fun ferie(fra: LocalDate, til: LocalDate, factory: DagFactory): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1))
                .map { ferie(it, factory) }
                .toList())
        }

        fun ikkeSykedager(
            fra: LocalDate,
            til: LocalDate,
            factory: DagFactory
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1))
                .map { ikkeSykedag(it, factory) }
                .toList())
        }

        fun utenlandsdager(
            fra: LocalDate,
            til: LocalDate,
            factory: DagFactory
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                utenlandsdag(
                    it, factory
                )
            }.toList())
        }

        fun studiedag(gjelder: LocalDate, factory: DagFactory) =
            if (!gjelder.erHelg()) factory.studiedag(gjelder) else factory.implisittDag(gjelder)

        fun studiedager(
            fra: LocalDate,
            til: LocalDate,
            factory: DagFactory
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                studiedag(
                    it, factory
                )
            }.toList())
        }

        fun permisjonsdag(gjelder: LocalDate, factory: DagFactory) =
            if (!gjelder.erHelg()) factory.permisjonsdag(gjelder) else factory.implisittDag(gjelder)

        internal fun implisittDag(gjelder: LocalDate, factory: DagFactory) = factory.implisittDag(gjelder)

        fun permisjonsdager(
            fra: LocalDate,
            til: LocalDate,
            factory: DagFactory
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { permisjonsdag(it, factory) }
                    .toList())
        }

        fun implisittdager(
            fra: LocalDate,
            til: LocalDate,
            factory: DagFactory
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { implisittDag(it, factory) }
                    .toList())
        }

        fun ubestemtdager(
            fra: LocalDate,
            til: LocalDate,
            factory: DagFactory
        ): ConcreteSykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { factory.ubestemtdag(it) }
                    .toList())
        }

        private fun beste(a: Dag?, b: Dag?, dagturnering: Dagturnering): Dag? {
            if (a == null) return b
            if (b == null) return a
            return a.beste(b, dagturnering)
        }
    }
}
