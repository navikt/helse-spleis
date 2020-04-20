package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Understands beginning and end of a time interval
class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate> {

    override val start: LocalDate = fom
    override val endInclusive: LocalDate = tom

    private val range = start..endInclusive

    init {
        require(start <= endInclusive) { "fom kan ikke være etter tom" }
    }

    internal fun overlapperMed(other: Periode) =
        maxOf(this.start, other.start) <= minOf(this.endInclusive, other.endInclusive)

    internal fun etter(other: LocalDate) =
        other <= this.endInclusive

    internal fun utenfor(other: Periode) =
        start < other.start || endInclusive > other.endInclusive

    private operator fun contains(other: Periode) =
        this.start < other.endInclusive && this.endInclusive > other.start

    internal operator fun contains(datoer: List<LocalDate>) = datoer.any { it in range }

    override fun toString(): String {
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }

    override fun equals(other: Any?) =
        other is Periode && this.equals(other)

    private fun equals(other: Periode) =
        this.start == other.start && this.endInclusive == other.endInclusive

    override fun hashCode() = start.hashCode() * 37 + endInclusive.hashCode()
    internal fun merge(annen: Periode?): Periode {
        if (annen == null) return this
        return Periode(minOf(this.start, annen.start), maxOf(this.endInclusive, annen.endInclusive))
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        fun List<Periode>.etter(grense: LocalDate) = any { it.etter(grense)}
    }
}
