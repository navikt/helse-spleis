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

    /**
     * Without padding of days
     */
    internal fun kutt(kuttDatoInclusive: LocalDate) =
        when {
            periode == null -> this
            kuttDatoInclusive < periode.start -> NySykdomstidslinje()
            kuttDatoInclusive > periode.endInclusive -> this
            else -> subset(Periode(periode.start, kuttDatoInclusive))
        }

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
        private val periodeIterator = periode?.iterator()

        override fun hasNext() = periodeIterator?.hasNext() ?: false

        override fun next() =
            periodeIterator?.let { this@NySykdomstidslinje[it.next()] }
                ?: throw NoSuchElementException()
    }
}
