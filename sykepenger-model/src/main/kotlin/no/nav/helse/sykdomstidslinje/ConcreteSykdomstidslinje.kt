package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.tournament.historiskDagturnering
import java.time.LocalDate
import kotlin.streams.toList

internal fun List<ConcreteSykdomstidslinje>.reduser(dagturnering: Dagturnering, inneklemtDag: (LocalDate) -> Dag = ::ImplisittDag) =
    reduce { result, other -> result.plus(other, dagturnering, inneklemtDag) }

internal interface SykdomstidslinjeElement {
    fun accept(visitor: SykdomstidslinjeVisitor)
}

internal abstract class ConcreteSykdomstidslinje : SykdomstidslinjeElement {
    abstract fun førsteDag(): LocalDate
    abstract fun sisteDag(): LocalDate
    internal abstract fun flatten(): List<Dag>
    internal abstract fun length(): Int
    internal abstract fun dag(dato: LocalDate): Dag?

    operator fun plus(other: ConcreteSykdomstidslinje) = this.plus(other, historiskDagturnering, ::ImplisittDag)
    fun plus(other: ConcreteSykdomstidslinje, dagturnering: Dagturnering, gapDayCreator: (LocalDate) -> Dag): ConcreteSykdomstidslinje {
        if (this.length() == 0) return other
        if (other.length() == 0) return this
        val førsteDag = this.førsteStartdato(other)
        val sisteDag = this.sisteSluttdato(other).plusDays(1)
        return CompositeSykdomstidslinje(førsteDag.datesUntil(sisteDag).map {
            beste(dagturnering, this.dag(it), other.dag(it)) ?: gapDayCreator(it)
        }.toList())
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

    fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        val ugyldigeDager = listOf(Permisjonsdag.Søknad::class, Permisjonsdag.Aareg::class, Ubestemtdag::class)
        return flatten().filter { it::class in ugyldigeDager }
            .distinctBy { it::class.simpleName }
            .onEach { aktivitetslogg.error("Sykdomstidslinjen inneholder ustøttet dag: %s", it::class.simpleName) }
            .isEmpty()
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

        private fun beste(dagturnering: Dagturnering, a: Dag?, b: Dag?): Dag? {
            if (a == null) return b
            if (b == null) return a
            return dagturnering.beste(a, b)
        }
    }
}
