package no.nav.helse.hendelser

import no.nav.helse.sykdomstidslinje.erRettFør
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Understands beginning and end of a time interval
class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate>, Iterable<LocalDate> {

    override val start: LocalDate = fom
    override val endInclusive: LocalDate = tom

    private val range = start..endInclusive

    init {
        require(start <= endInclusive) { "fom kan ikke være etter tom" }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        fun List<Periode>.slutterEtter(grense: LocalDate) = any { it.slutterEtter(grense) }

        internal fun List<Periode>.slåSammen() =
            sortedBy { it.start }
                .fold(emptyList<Periode>()) { perioder, periode ->
                    if (perioder.isEmpty()) return@fold listOf(periode)

                    val siste = perioder.last()
                    if (siste.overlapperMed(periode) || siste.erRettFør(periode)) {
                        perioder.dropLast(1).plusElement(siste.merge(periode))
                    } else {
                        perioder.plusElement(periode)
                    }
                }
    }

    internal fun overlapperMed(other: Periode) =
        maxOf(this.start, other.start) <= minOf(this.endInclusive, other.endInclusive)

    internal fun slutterEtter(other: LocalDate) =
        other <= this.endInclusive

    internal fun utenfor(other: Periode) =
        this.start < other.start || this.endInclusive > other.endInclusive

    internal fun erRettFør(other: Periode) = this.endInclusive.erRettFør(other.start)

    internal operator fun contains(other: Periode) =
        this.start <= other.start && this.endInclusive >= other.endInclusive

    internal operator fun contains(datoer: List<LocalDate>) = datoer.any { it in range }

    override fun toString(): String {
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }

    internal fun oppdaterFom(other: Periode) = Periode(minOf(this.start, other.start), endInclusive)

    override fun equals(other: Any?) =
        other is Periode && this.equals(other)

    private fun equals(other: Periode) =
        this.start == other.start && this.endInclusive == other.endInclusive

    override fun hashCode() = start.hashCode() * 37 + endInclusive.hashCode()

    internal fun merge(annen: Periode?): Periode {
        if (annen == null) return this
        return Periode(minOf(this.start, annen.start), maxOf(this.endInclusive, annen.endInclusive))
    }

    override operator fun iterator() = object : Iterator<LocalDate> {
        private var currentDate: LocalDate = start

        override fun hasNext() = endInclusive >= currentDate

        override fun next() =
            currentDate.also { currentDate = it.plusDays(1) }
    }
}

internal operator fun List<Periode>.contains(dato: LocalDate) = this.any { dato in it }

internal infix fun LocalDate.til(januar: LocalDate) = Periode(this, januar)
