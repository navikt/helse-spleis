package no.nav.helse.hendelser

import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import no.nav.helse.erRettFør
import no.nav.helse.forrigeDag
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.nesteDag
import kotlin.collections.plus

// Understands beginning and end of a time interval
class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate>, Iterable<LocalDate> {

    override val start: LocalDate = fom
    override val endInclusive: LocalDate = tom

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
        fun Iterable<LocalDate>.grupperSammenhengendePerioderMedHensynTilHelg() = map(LocalDate::somPeriode).merge(
            mergeOverHelg
        )
        fun List<Periode>.grupperSammenhengendePerioder() = merge(mergeKantIKant)
        fun List<Periode>.grupperSammenhengendePerioderMedHensynTilHelg() = merge(mergeOverHelg)

        fun Iterable<Periode>.merge(nyPeriode: Periode) = this
            .flatMap { it.trim(nyPeriode) }
            .plusElement(nyPeriode)
            .sortedBy { it.start }

        /*
            bryter <other> opp i ulike biter som ikke dekkes av listen av perioder.
            antar at listen av perioder er sortert
         */
        fun Iterable<Periode>.trim(other: Periode): List<Periode> =
            fold(listOf(other)) { result, trimperiode ->
                result.dropLast(1) + (result.lastOrNull()?.trim(trimperiode) ?: emptyList())
            }

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

        fun Iterable<Periode>.intersect(other: Iterable<Periode>): List<Periode> =
            flatten().intersect(other.flatten().toSet()).grupperSammenhengendePerioder()

        fun List<Periode>.lik(other: List<Periode>): Boolean {
            if (size != other.size) return false
            return sortedBy { it.start } == other.sortedBy { it.start }
        }

        fun Periode.delvisOverlappMed(other: Periode) = overlapperMed(other) && !inneholder(other)

        fun gjenopprett(dto: PeriodeDto) = Periode(
            fom = dto.fom,
            tom = dto.tom
        )
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

    fun utenHelgehale(): Periode? {
        val nåværendeTom = endInclusive
        val ukedag = endInclusive.dayOfWeek
        if (ukedag !in setOf(SATURDAY, SUNDAY)) return this
        val nyTom = nåværendeTom.minusDays(if (ukedag == SATURDAY) 1 else 2)
        if (nyTom < start) return null
        return start til nyTom
    }

    fun beholdDagerEtter(cutoff: LocalDate): Periode? = when {
        endInclusive <= cutoff -> null
        start > cutoff -> this
        else -> cutoff.plusDays(1) til endInclusive
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

    fun trimDagerFør(other: Periode) = this.trim(other.oppdaterTom(LocalDate.MAX)).periode()

    private fun beholdDagerFør(other: Periode) = this.start til other.start.forrigeDag
    private fun beholdDagerEtter(other: Periode) = other.endInclusive.nesteDag til this.endInclusive

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

    fun dto() = PeriodeDto(start, endInclusive)
}

operator fun List<Periode>.contains(dato: LocalDate) = this.any { dato in it }

infix fun LocalDate.til(tom: LocalDate) = Periode(this, tom)
fun LocalDate.somPeriode() = Periode(this, this)
