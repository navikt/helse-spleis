package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import no.nav.helse.erRettFør
import no.nav.helse.nesteArbeidsdag
import no.nav.helse.nesteDag

// Understands beginning and end of a time interval
open class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate>, Iterable<LocalDate> {

    final override val start: LocalDate = fom
    final override val endInclusive: LocalDate = tom

    init {
        require(start <= endInclusive) { "fom ($start) kan ikke være etter tom ($endInclusive)" }
    }

    companion object {
        internal val aldri = LocalDate.MIN til LocalDate.MIN

        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        fun List<Periode>.slutterEtter(grense: LocalDate) = any { it.slutterEtter(grense) }

        fun Iterable<LocalDate>.grupperSammenhengendePerioder() = this
            .grupperSammenhengendePerioder { forrigeDag, dato -> forrigeDag.nesteDag != dato }
        internal fun Iterable<Iterable<LocalDate>>.grupperSammenhengendePerioder() = flatten().grupperSammenhengendePerioder()
        internal fun Iterable<Periode>.periode() = if (!iterator().hasNext()) null else minOf { it.start } til maxOf { it.endInclusive }
        fun List<Periode>.grupperSammenhengendePerioderMedHensynTilHelg() = this
            .flatMap { periode -> periode.map { it } }
            .grupperSammenhengendePerioder { forrigeDag, dato -> !forrigeDag.erRettFør(dato) }

        private fun Iterable<LocalDate>.grupperSammenhengendePerioder(erNyPeriode: (forrigeDag: LocalDate, nesteDag: LocalDate) -> Boolean): List<Periode> {
            val perioder = mutableListOf<Periode>()
            toSortedSet().forEach { dato ->
                val sistePeriode = perioder.lastOrNull()
                when {
                    sistePeriode == null || erNyPeriode(sistePeriode.endInclusive, dato) -> perioder.add(dato.somPeriode())
                    else -> perioder[perioder.lastIndex] = sistePeriode.oppdaterTom(dato)
                }
            }
            return perioder
        }

        internal fun Collection<Periode>.sammenhengende(skjæringstidspunkt: LocalDate) = sortedByDescending { it.start }
            .fold(skjæringstidspunkt til skjæringstidspunkt) { acc, periode ->
                if (periode.rettFørEllerOverlapper(acc.start)) periode.start til acc.endInclusive
                else acc
            }

        internal fun Iterable<Periode>.overlapper(): Boolean {
            sortedBy { it.start }.zipWithNext { nåværende, neste ->
                if (nåværende.overlapperMed(neste)) return true
            }
            return false
        }
    }

    fun overlapperMed(other: Periode) =
        maxOf(this.start, other.start) <= minOf(this.endInclusive, other.endInclusive)

    internal fun slutterEtter(other: LocalDate) =
        other <= this.endInclusive

    internal fun utenfor(other: Periode) =
        this.start < other.start || this.endInclusive > other.endInclusive

    internal fun inneholder(other: Periode) = other in this

    internal fun erRettFør(other: Periode) = erRettFør(other.start)
    internal fun erRettFør(other: LocalDate) = this.endInclusive.erRettFør(other)

    private fun rettFørEllerOverlapper(dato: LocalDate) = start < dato && endInclusive.nesteArbeidsdag() >= dato
    internal fun dagerMellom() = ChronoUnit.DAYS.between(start, endInclusive)

    internal fun periodeMellom(other: LocalDate): Periode? {
        val enDagFør = other.minusDays(1)
        if (slutterEtter(enDagFør)) return null
        return Periode(endInclusive.plusDays(1), enDagFør)
    }

    internal operator fun contains(other: Periode) =
        this.start <= other.start && this.endInclusive >= other.endInclusive

    internal operator fun contains(datoer: Iterable<LocalDate>) = datoer.any { it in this }

    override fun toString(): String {
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }

    internal fun oppdaterFom(other: LocalDate) = Periode(minOf(this.start, other), endInclusive)
    internal fun oppdaterFom(other: Periode) = oppdaterFom(other.start)
    internal fun oppdaterTom(other: LocalDate) = Periode(this.start, maxOf(other, this.endInclusive))
    internal fun oppdaterTom(other: Periode) = oppdaterTom(other.endInclusive)

    internal fun beholdDagerEtter(cutoff: LocalDate): Periode? = when {
        endInclusive <= cutoff -> null
        start > cutoff -> this
        else -> cutoff.plusDays(1) til endInclusive
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

    override operator fun iterator() = object : Iterator<LocalDate> {
        private var currentDate: LocalDate = start

        override fun hasNext() = endInclusive >= currentDate

        override fun next() =
            currentDate.also { currentDate = it.plusDays(1) }
    }

    internal fun subset(periode: Periode) =
        Periode(start.coerceAtLeast(periode.start), endInclusive.coerceAtMost(periode.endInclusive))
}

internal operator fun List<Periode>.contains(dato: LocalDate) = this.any { dato in it }

internal infix fun LocalDate.til(tom: LocalDate) = Periode(this, tom)
internal fun LocalDate.somPeriode() = Periode(this, this)
