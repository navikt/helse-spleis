package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.tournament.historiskDagturnering
import java.time.LocalDate
import kotlin.streams.toList

// Understands a specific period that probably involves illness
internal class Sykdomstidslinje private constructor(private val dager: List<Dag>) {

    internal constructor(): this(mutableListOf<Dag>())

    companion object {
        internal fun sykedager(fra: LocalDate, til: LocalDate, grad: Double, factory: DagFactory): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return Sykdomstidslinje(fra.datesUntil(til.plusDays(1))
                .map {sykedag(it, grad, factory) }
                .toList())
        }

        private fun sykedag(gjelder: LocalDate, grad: Double, factory: DagFactory): Dag =
            if (!gjelder.erHelg()) factory.sykedag(gjelder, grad) else factory.sykHelgedag(gjelder, grad)

        internal fun ikkeSykedag(gjelder: LocalDate, factory: DagFactory) =
            if (!gjelder.erHelg()) factory.arbeidsdag(gjelder) else factory.implisittDag(gjelder)

        private fun beste(dagturnering: Dagturnering, a: Dag?, b: Dag?): Dag? {
            if (a == null) return b
            if (b == null) return a
            return dagturnering.beste(a, b)
        }
    }

    internal val size get() = dager.size

    internal fun merge(other: Sykdomstidslinje, dagturnering: Dagturnering, gapDayCreator: (LocalDate) -> Dag = ::ImplisittDag): Sykdomstidslinje {
        return kobleSammen(other) {
            beste(dagturnering, this.dag(it), other.dag(it)) ?: gapDayCreator(it)
        }
    }

    operator fun plus(other: Sykdomstidslinje) = merge(other, historiskDagturnering)

    private fun kobleSammen(other: Sykdomstidslinje, mapper: (LocalDate) -> Dag): Sykdomstidslinje {
        if (other.size == 0) return this
        if (this.size == 0) return other
        val førsteDag = this.førsteStartdato(other)
        val sisteDag = this.sisteSluttdato(other).plusDays(1)
        return Sykdomstidslinje(førsteDag.datesUntil(sisteDag).map(mapper).toList())
    }

    private fun førsteStartdato(other: Sykdomstidslinje) =
        if (this.førsteDag().isBefore(other.førsteDag())) this.førsteDag() else other.førsteDag()

    private fun sisteSluttdato(other: Sykdomstidslinje) =
        if (this.sisteDag().isAfter(other.sisteDag())) this.sisteDag() else other.sisteDag()

    private fun førsteDag() = dager.first().dagen

    private fun sisteDag() = dager.last().dagen

    private fun dag(dato: LocalDate) = dager.find { it.dagen == dato }

    operator fun get(dato: LocalDate) = dag(dato)

    override fun toString(): String {
        if (dager.isEmpty()) return "<empty>"
        return dager.joinToString(separator = "\n")
    }

    internal fun toShortString(): String {
        return dager.joinToString(separator = "") {
            when (it::class) {
                Sykedag.Søknad::class -> "S"
                Sykedag.Sykmelding::class -> "S"
                Arbeidsdag.Inntektsmelding::class -> "A"
                Arbeidsdag.Søknad::class -> "A"
                ImplisittDag::class -> "I"
                else -> "?"
            }
        }
    }
}
