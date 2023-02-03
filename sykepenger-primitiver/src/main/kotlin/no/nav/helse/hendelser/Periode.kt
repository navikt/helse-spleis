package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import no.nav.helse.erRettFør
import no.nav.helse.nesteDag

// Understands beginning and end of a time interval
open class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate>, Iterable<LocalDate> {

    final override val start: LocalDate = fom
    final override val endInclusive: LocalDate = tom

    init {
        require(start <= endInclusive) { "fom ($start) kan ikke være etter tom ($endInclusive)" }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private val mergeOverHelg = { venstre: LocalDate, høyre: LocalDate ->
            venstre.erRettFør(høyre)
        }
        private val mergeKantIKant = { venstre: LocalDate, høyre: LocalDate ->
            venstre.nesteDag == høyre
        }

        fun List<Periode>.slutterEtter(grense: LocalDate) = any { it.slutterEtter(grense) }

        fun Iterable<Periode>.periode() = if (!iterator().hasNext()) null else minOf { it.start } til maxOf { it.endInclusive }
        fun Iterable<LocalDate>.grupperSammenhengendePerioder() = map(LocalDate::somPeriode).merge(
            mergeKantIKant
        )
        private fun Iterable<LocalDate>.grupperSammenhengendePerioderMedHensynTilHelg() = map(LocalDate::somPeriode).merge(
            mergeOverHelg
        )
        fun List<Periode>.grupperSammenhengendePerioder() = merge(mergeKantIKant)
        fun List<Periode>.grupperSammenhengendePerioderMedHensynTilHelg() = merge(mergeOverHelg)

        fun List<Periode>.sammenhengende(dato: LocalDate) = this
            .grupperSammenhengendePerioderMedHensynTilHelg()
            .firstOrNull { dato in it || it.erRettFør(dato) }
            ?.let { it.start til dato }
            ?: dato.somPeriode()

        val Iterable<LocalDate>.omsluttendePeriode get() = this.takeIf { it.iterator().hasNext() }?.let { min() til max() }

        fun Iterable<LocalDate>.periodeRettFør(dato: LocalDate): Periode? {
            val rettFør = sorted().lastOrNull { it.erRettFør(dato) } ?: return null
            return grupperSammenhengendePerioderMedHensynTilHelg().single { rettFør in it }.let { periode ->
                periode.subset(periode.start til rettFør)
            }
        }

        private fun Iterable<Periode>.merge(erForlengelse: (forrigeDag: LocalDate, nesteDag: LocalDate) -> Boolean = mergeKantIKant): List<Periode> {
            val resultat = mutableListOf<Periode>()
            val sortert = sortedBy { it.start }
            sortert.forEachIndexed { index, periode ->
                if (resultat.any { champion -> periode in champion }) return@forEachIndexed // en annen periode har spist opp denne
                resultat.add(sortert.subList(index, sortert.size).reduce { champion, challenger ->
                    champion.merge(challenger, erForlengelse)
                })
            }
            return resultat
        }

        fun Iterable<Periode>.overlapper(): Boolean {
            sortedBy { it.start }.zipWithNext { nåværende, neste ->
                if (nåværende.overlapperMed(neste)) return true
            }
            return false
        }

        fun Periode.delvisOverlappMed(other: Periode) = overlapperMed(other) && !inneholder(other)
    }

    fun overlapperMed(other: Periode) = overlappendePeriode(other) != null

    fun overlappendePeriode(other: Periode): Periode? {
        val start = maxOf(this.start, other.start)
        val slutt = minOf(this.endInclusive, other.endInclusive)
        if (start > slutt) return null
        return start til slutt
    }

    fun slutterEtter(other: LocalDate) =
        other <= this.endInclusive

    fun utenfor(other: Periode) =
        this.start < other.start || this.endInclusive > other.endInclusive

    fun starterEtter(other: Periode) =
        this.start > other.endInclusive

    fun inneholder(other: Periode) = other in this

    fun erRettFør(other: Periode) = erRettFør(other.start)
    fun erRettFør(other: LocalDate) = this.endInclusive.erRettFør(other)

    fun periodeMellom(other: LocalDate): Periode? {
        val enDagFør = other.minusDays(1)
        if (slutterEtter(enDagFør)) return null
        return Periode(endInclusive.plusDays(1), enDagFør)
    }

    operator fun contains(other: Periode) =
        this.start <= other.start && this.endInclusive >= other.endInclusive

    operator fun contains(datoer: Iterable<LocalDate>) = datoer.any { it in this }

    override fun toString(): String {
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }

    fun oppdaterFom(other: LocalDate) = Periode(minOf(this.start, other), endInclusive)
    fun oppdaterFom(other: Periode) = oppdaterFom(other.start)
    fun oppdaterTom(other: LocalDate) = Periode(this.start, maxOf(other, this.endInclusive))
    fun oppdaterTom(other: Periode) = oppdaterTom(other.endInclusive)

    fun beholdDagerEtter(cutoff: LocalDate): Periode? = when {
        endInclusive <= cutoff -> null
        start > cutoff -> this
        else -> cutoff.plusDays(1) til endInclusive
    }

    override fun equals(other: Any?) =
        other is Periode && this.equals(other)

    private fun equals(other: Periode) =
        this.start == other.start && this.endInclusive == other.endInclusive

    override fun hashCode() = start.hashCode() * 37 + endInclusive.hashCode()

    fun merge(other: Periode, erForlengelseStrategy: (LocalDate, LocalDate) -> Boolean): Periode {
        if (this.overlapperMed(other) || erForlengelseStrategy(this.endInclusive, other.start) || erForlengelseStrategy(other.endInclusive, this.start)) {
            return this + other
        }
        return this
    }

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
}

operator fun List<Periode>.contains(dato: LocalDate) = this.any { dato in it }

infix fun LocalDate.til(tom: LocalDate) = Periode(this, tom)
infix fun LocalDate.tilOrNull(tom: LocalDate) = if (tom < this) null else this til tom
fun LocalDate.somPeriode() = Periode(this, this)
