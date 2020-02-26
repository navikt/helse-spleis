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
        this.start in other || this.endInclusive in other || this in other || other in this

    private operator fun contains(other: Periode) =
        this.start < other.endInclusive && this.endInclusive > other.start

    internal operator fun contains(datoer: List<LocalDate>) = datoer.any { it in range }

    override fun toString(): String {
        var formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }
}
