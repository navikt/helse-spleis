package no.nav.helse.person.beløp

import java.time.LocalDate
import java.util.SortedMap
import no.nav.helse.hendelser.til
import no.nav.helse.økonomi.Inntekt

data class Beløpstidslinje private constructor(private val dager: SortedMap<LocalDate, Beløpsdag>) : Iterable<Dag> {

    private val periode = if (dager.isEmpty()) null else dager.firstKey() til dager.lastKey()

    internal constructor(dager: List<Beløpsdag>): this(dager.associateBy { it.dato }.toSortedMap().also {
        require(dager.size == it.size) { "Forsøkte å opprette en beløpstidslinje med duplikate datoer. Det blir for rart for meg." }
    })

    internal operator fun get(dato: LocalDate): Dag = dager[dato] ?: UkjentDag

    override operator fun iterator(): Iterator<Dag> {
        if (periode == null) return emptyList<Nothing>().iterator()
        return object : Iterator<Dag> {
            private val periodeIterator = periode.iterator()
            override fun hasNext() = periodeIterator.hasNext()
            override fun next() = this@Beløpstidslinje[periodeIterator.next()]
        }
    }

    internal operator fun plus(other: Beløpstidslinje): Beløpstidslinje{
        return Beløpstidslinje((this.dager + other.dager).toSortedMap())
    }
}

sealed interface Dag {
    val dato: LocalDate
    val beløp: Inntekt
    val kilde: Kilde
}

data class Beløpsdag(override val dato: LocalDate, override val beløp: Inntekt, override val kilde: Kilde) : Dag {}

data object UkjentDag : Dag {
    override val dato: LocalDate
        get() = error("En ukjent dag har ikke en dato")
    override val beløp: Inntekt
        get() = error("En ukjent dag har ikke et beløp")
    override val kilde: Kilde
        get() = error("En ukjent dag har ikke et beløp")
}
