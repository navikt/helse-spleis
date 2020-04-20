package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.NyDag.NyArbeidsdag
import no.nav.helse.sykdomstidslinje.NyDag.NyUkjentDag
import java.time.LocalDate
import java.util.*
import java.util.stream.Collectors.toMap

internal class NySykdomstidslinje private constructor(
    private val dager: SortedMap<LocalDate, NyDag>,
    private val periode: Periode? = periode1(dager)
) : Iterable<NyDag> {

    internal constructor(dager: Map<LocalDate, NyDag> = emptyMap()) : this(
        dager.toSortedMap()
    )

    internal fun periode() = periode

    internal fun merge(annen: NySykdomstidslinje): NySykdomstidslinje {
        val nySykdomstidlinje = mutableMapOf<LocalDate, NyDag>()
        dager.toMap(nySykdomstidlinje)
        nySykdomstidlinje.putAll(annen.dager)
        return NySykdomstidslinje(
            nySykdomstidlinje.toSortedMap(),
            this.periode?.merge(annen.periode) ?: annen.periode
        )
    }

    operator fun get(dato: LocalDate): NyDag = dager[dato] ?: NyUkjentDag(dato)

    internal fun subset(periode: Periode) =
        NySykdomstidslinje(dager.filter { it.key in periode }.toSortedMap(), periode)

    internal companion object {

        private fun periode1(dager: SortedMap<LocalDate, NyDag>) =
            if (dager.size > 0) Periode(dager.firstKey(), dager.lastKey()) else null

        internal fun arbeidsdager(førsteDato: LocalDate, sisteDato: LocalDate) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(toMap<LocalDate, LocalDate, NyDag>({ it }, { NyArbeidsdag(it) }))
            )
    }

    override operator fun iterator() = object : Iterator<NyDag> {
        private var currentDate: LocalDate? = periode?.start

        override fun hasNext() = periode?.let { periode.endInclusive >= currentDate } ?: false

        override fun next() =
            currentDate?.let { this@NySykdomstidslinje[it.also { currentDate = it.plusDays(1) }] }
                ?: throw NoSuchElementException()
    }
}
