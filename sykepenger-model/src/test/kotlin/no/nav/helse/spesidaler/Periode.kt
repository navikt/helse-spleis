package no.nav.helse.spesidaler

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**  Dette er bare en rå kopi fra spleis-modellkoden (fjernet unused ting) **/
class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate> {
    override val start: LocalDate = fom
    override val endInclusive: LocalDate = tom

    init {
        require(start <= endInclusive) { "fom ($start) kan ikke være etter tom ($endInclusive)" }
    }

    internal companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        internal fun Iterable<Periode>.trim(other: Periode): List<Periode> =
            fold(listOf(other)) { result, trimperiode ->
                result.dropLast(1) + (result.lastOrNull()?.trim(trimperiode) ?: emptyList())
            }
    }

    internal fun overlapperMed(other: Periode) = overlappendePeriode(other) != null

    internal fun overlappendePeriode(other: Periode): Periode? {
        val start = maxOf(this.start, other.start)
        val slutt = minOf(this.endInclusive, other.endInclusive)
        if (start > slutt) return null
        return start til slutt
    }

    override fun toString(): String {
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }

    private fun trim(other: Periode): List<Periode> {
        val felles = this.overlappendePeriode(other) ?: return listOf(this)
        return when {
            felles == this -> emptyList()
            felles.start == this.start -> listOf(this.beholdDagerEtter(felles))
            felles.endInclusive == this.endInclusive -> listOf(this.beholdDagerFør(felles))
            else -> listOf(this.beholdDagerFør(felles), this.beholdDagerEtter(felles))
        }
    }

    private fun beholdDagerFør(other: Periode) = this.start til other.start.minusDays(1)

    private fun beholdDagerEtter(other: Periode) = other.endInclusive.plusDays(1) til this.endInclusive

    override fun equals(other: Any?): Boolean {
        if (other !is Periode) return false
        if (this === other) return true
        return this.start == other.start && this.endInclusive == other.endInclusive
    }

    override fun hashCode() = start.hashCode() + endInclusive.hashCode()
}

infix fun LocalDate.til(tom: LocalDate) = Periode(this, tom)
