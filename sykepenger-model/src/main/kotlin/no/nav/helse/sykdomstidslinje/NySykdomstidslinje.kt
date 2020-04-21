package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.dag.erHelg
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors.toMap

internal class NySykdomstidslinje private constructor(
    private val dager: SortedMap<LocalDate, NyDag>,
    private val periode: Periode? = periode1(dager),
    private val id: UUID = UUID.randomUUID(),
    private val tidsstempel: LocalDateTime = LocalDateTime.now()
) : Iterable<NyDag> {

    internal constructor(dager: Map<LocalDate, NyDag> = emptyMap()) : this(
        dager.toSortedMap()
    )

    internal fun periode() = periode

    internal fun merge(annen: NySykdomstidslinje, beste: BesteStrategy = NyDag.default): NySykdomstidslinje {
        val dager = mutableMapOf<LocalDate, NyDag>()
        this.dager.toMap(dager)
        annen.dager.forEach { (dato, dag) -> dager.merge(dato, dag, beste) }
        return NySykdomstidslinje(
            dager.toSortedMap(),
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
                    .collect(
                        toMap<LocalDate, LocalDate, NyDag>(
                            { it },
                            { if (it.erHelg()) NyFriskHelgedag(it) else NyArbeidsdag(it) })
                    )
            )

        internal fun sykedager(førsteDato: LocalDate, sisteDato: LocalDate, grad: Number = 100.0) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, NyDag>(
                            { it },
                            { if (it.erHelg()) NySykHelgedag(it) else NySykedag(it, grad) })
                    )
            )

        internal fun arbeidsgiverdager(førsteDato: LocalDate, sisteDato: LocalDate, grad: Number = 100.0) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, NyDag>(
                            { it },
                            { if (it.erHelg()) NyArbeidsgiverHelgedag(it) else NyArbeidsgiverdag(it, grad) })
                    )
            )

        internal fun feriedager(førsteDato: LocalDate, sisteDato: LocalDate) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(toMap<LocalDate, LocalDate, NyDag>({ it }, { NyFeriedag(it) }))
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
