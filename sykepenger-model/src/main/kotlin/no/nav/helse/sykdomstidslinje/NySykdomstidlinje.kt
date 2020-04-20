package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import java.time.LocalDate
import kotlin.streams.toList

internal class NySykdomstidlinje(dager: Map<LocalDate, NyArbeidsdag> = emptyMap()) {
    private val dager = dager.toSortedMap()
    internal fun periode(): Periode? = if (dager.size > 0) Periode(dager.firstKey(), dager.lastKey()) else null

    internal fun merge(annenSykdomstidlinje: NySykdomstidlinje): NySykdomstidlinje {
        val nySykdomstidlinje = mutableMapOf<LocalDate, NyArbeidsdag>()
        dager.toMap(nySykdomstidlinje)
        nySykdomstidlinje.putAll(annenSykdomstidlinje.dager)
        return NySykdomstidlinje(nySykdomstidlinje)
    }

    operator fun get(dato: LocalDate): NyArbeidsdag = dager[dato]!!

    internal companion object {
        internal fun arbeidsdager(førsteDato: LocalDate, sisteDato: LocalDate) =
            NySykdomstidlinje(førsteDato.datesUntil(sisteDato.plusDays(1)).toList().map { it to NyArbeidsdag(it) }.toMap())
    }
}
