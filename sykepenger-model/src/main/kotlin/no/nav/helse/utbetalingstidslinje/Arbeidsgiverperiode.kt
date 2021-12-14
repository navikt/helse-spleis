package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import java.time.LocalDate

internal class Arbeidsgiverperiode(private val perioder: List<Periode>) : Iterable<LocalDate>, Comparable<LocalDate> {
    init {
        check(perioder.isNotEmpty())
    }

    private val hele = perioder.first().start til perioder.last().endInclusive

    override fun compareTo(other: LocalDate) =
        perioder.first().start.compareTo(other)

    operator fun contains(dato: LocalDate) =
        dato in hele

    operator fun contains(periode: Periode) =
        periode.overlapperMed(hele)

    internal fun dekker(periode: Periode) =
        hele.inneholder(periode)

    override fun iterator(): Iterator<LocalDate> {
        return object : Iterator<LocalDate> {
            private val periodeIterators = perioder.map { it.iterator() }.iterator()
            private var current: Iterator<LocalDate>? = null

            override fun hasNext(): Boolean {
                val iterator = current
                if (iterator != null && iterator.hasNext()) return true
                if (!periodeIterators.hasNext()) return false
                current = periodeIterators.next()
                return true
            }

            override fun next(): LocalDate {
                return current?.next() ?: throw NoSuchElementException()
            }
        }
    }
}
