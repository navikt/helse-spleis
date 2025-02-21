package no.nav.helse.spesidaler

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import no.nav.helse.dto.PeriodeDto

/**  Dette er bare en rå kopi fra spleis-modellkoden (fjernet unused ting) **/
class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate>, Iterable<LocalDate> {

    override val start: LocalDate = fom
    override val endInclusive: LocalDate = tom

    init {
        require(start <= endInclusive) { "fom ($start) kan ikke være etter tom ($endInclusive)" }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        fun Iterable<Periode>.periode() = if (!iterator().hasNext()) null else minOf { it.start } til maxOf { it.endInclusive }


        /*
            bryter <other> opp i ulike biter som ikke dekkes av listen av perioder.
            antar at listen av perioder er sortert
         */
        fun Iterable<Periode>.trim(other: Periode): List<Periode> =
            fold(listOf(other)) { result, trimperiode ->
                result.dropLast(1) + (result.lastOrNull()?.trim(trimperiode) ?: emptyList())
            }


        fun Iterable<Periode>.overlapper(): Boolean {
            sortedBy { it.start }.zipWithNext { nåværende, neste ->
                if (nåværende.overlapperMed(neste)) return true
            }
            return false
        }

        fun List<Periode>.lik(other: List<Periode>): Boolean {
            if (size != other.size) return false
            return sortedBy { it.start } == other.sortedBy { it.start }
        }
    }

    fun overlapperMed(other: Periode) = overlappendePeriode(other) != null

    fun overlappendePeriode(other: Periode): Periode? {
        val start = maxOf(this.start, other.start)
        val slutt = minOf(this.endInclusive, other.endInclusive)
        if (start > slutt) return null
        return start til slutt
    }

    fun utenfor(other: Periode) =
        this.start < other.start || this.endInclusive > other.endInclusive

    fun inneholder(other: Periode) = other in this

    operator fun contains(other: Periode) =
        this.start <= other.start && this.endInclusive >= other.endInclusive

    operator fun contains(datoer: Iterable<LocalDate>) = datoer.any { it in this }

    override fun toString(): String {
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }

    fun trim(other: Periode): List<Periode> {
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

    override fun equals(other: Any?) =
        other is Periode && this.equals(other)

    private fun equals(other: Periode) =
        this.start == other.start && this.endInclusive == other.endInclusive

    override fun hashCode() = start.hashCode() * 37 + endInclusive.hashCode()

    operator fun plus(annen: Periode?): Periode {
        if (annen == null) return this
        return Periode(minOf(this.start, annen.start), maxOf(this.endInclusive, annen.endInclusive))
    }

    override operator fun iterator() = object : Iterator<LocalDate> {
        private var currentDate: LocalDate = start

        override fun hasNext() = endInclusive >= currentDate

        override fun next() =
            currentDate.also { currentDate = it.plusDays(1) }
    }

    fun subset(periode: Periode) =
        Periode(start.coerceAtLeast(periode.start), endInclusive.coerceAtMost(periode.endInclusive))

    fun dto() = PeriodeDto(start, endInclusive)
}

operator fun List<Periode>.contains(dato: LocalDate) = this.any { dato in it }

infix fun LocalDate.til(tom: LocalDate) = Periode(this, tom)
fun LocalDate.somPeriode() = Periode(this, this)
