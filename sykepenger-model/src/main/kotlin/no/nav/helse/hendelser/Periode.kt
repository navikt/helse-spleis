package no.nav.helse.hendelser

import no.nav.helse.sykdomstidslinje.erRettFør
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Understands beginning and end of a time interval
open class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate>, Iterable<LocalDate> {

    override val start: LocalDate = fom
    override val endInclusive: LocalDate = tom

    init {
        require(start <= endInclusive) { "fom ($start) kan ikke være etter tom ($endInclusive)" }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        fun List<Periode>.slutterEtter(grense: LocalDate) = any { it.slutterEtter(grense) }

        internal fun List<LocalDate>.merge() = sorted().fold(mutableListOf(), ::utvidForrigeEllerNy)
        private fun utvidForrigeEllerNy(perioder: MutableList<Periode>, dato: LocalDate): MutableList<Periode> {
            val siste = perioder.lastOrNull()
            if (siste == null || (dato !in siste && siste.endInclusive.plusDays(1) != dato)) perioder.add(dato til dato)
            else perioder[perioder.size - 1] = siste.oppdaterTom(dato)
            return perioder
        }
    }

    internal fun overlapperMed(other: Periode) =
        maxOf(this.start, other.start) <= minOf(this.endInclusive, other.endInclusive)

    internal fun slutterEtter(other: LocalDate) =
        other <= this.endInclusive

    internal fun utenfor(other: Periode) =
        this.start < other.start || this.endInclusive > other.endInclusive

    internal fun erRettFør(other: Periode) = erRettFør(other.start)
    internal fun erRettFør(other: LocalDate) = this.endInclusive.erRettFør(other)

    internal fun periodeMellom(other: LocalDate): Periode? {
        val enDagFør = other.minusDays(1)
        if (slutterEtter(enDagFør)) return null
        return Periode(endInclusive.plusDays(1), enDagFør)
    }

    internal operator fun contains(other: Periode) =
        this.start <= other.start && this.endInclusive >= other.endInclusive

    internal operator fun contains(datoer: List<LocalDate>) = datoer.any { it in this }

    override fun toString(): String {
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }

    internal fun oppdaterFom(other: LocalDate) = Periode(minOf(this.start, other), endInclusive)
    internal fun oppdaterFom(other: Periode) = oppdaterFom(other.start)
    internal fun oppdaterTom(other: LocalDate) = Periode(this.start, maxOf(other, this.endInclusive))
    internal fun oppdaterTom(other: Periode) = oppdaterTom(other.endInclusive)

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

internal infix fun LocalDate.til(tom: LocalDate) = Periode(this, tom)

