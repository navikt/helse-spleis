package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.sykdomstidslinje.erRettFør
import java.time.LocalDate

internal class Arbeidsgiverperiode(private val perioder: List<Periode>) : Iterable<LocalDate>, Comparable<LocalDate> {
    init {
        check(perioder.isNotEmpty())
    }

    private val første = perioder.first().start
    private val hele = perioder.first().start til perioder.last().endInclusive

    override fun compareTo(other: LocalDate) =
        første.compareTo(other)

    operator fun contains(dato: LocalDate) =
        dato in hele

    operator fun contains(periode: Periode) =
        periode.overlapperMed(hele)

    internal fun dekker(periode: Periode) =
        periode in this && hele.slutterEtter(periode.endInclusive)

    internal fun hørerTil(periode: Periode, sisteKjente: LocalDate) =
        periode.overlapperMed(første til sisteKjente)

    internal fun sammenlign(other: List<Periode>): Boolean {
        val otherSiste = other.lastOrNull()?.endInclusive ?: return false
        val thisSiste = this.perioder.last().endInclusive
        return otherSiste == thisSiste || (thisSiste.erHelg() && otherSiste.erRettFør(thisSiste)) || (otherSiste.erHelg() && thisSiste.erRettFør(otherSiste))
    }

    override fun equals(other: Any?) = other is Arbeidsgiverperiode && other.første == this.første
    override fun hashCode() = første.hashCode()

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
