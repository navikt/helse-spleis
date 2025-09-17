package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDate.EPOCH
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til

internal class Arbeidsgiverperiode(private val perioder: List<Periode>) : Iterable<LocalDate>, Comparable<LocalDate> {
    private val kjenteDager = mutableListOf<Periode>()
    private val utbetalingsdager = mutableListOf<Periode>()
    private val oppholdsdager = mutableListOf<Periode>()

    init {
        check(perioder.isNotEmpty()) {
            "Arbeidsgiverperioden må være oppgitt"
        }
    }

    private val arbeidsgiverperioden get() = perioder.first().start til perioder.last().endInclusive
    private val førsteKjente get() = listOfNotNull(perioder.firstOrNull()?.start, utbetalingsdager.firstOrNull()?.start, kjenteDager.firstOrNull()?.start).minOf { it }
    private val sisteKjente get() = listOfNotNull(perioder.lastOrNull()?.endInclusive, utbetalingsdager.lastOrNull()?.endInclusive, kjenteDager.lastOrNull()?.endInclusive).maxOf { it }
    private val innflytelseperioden get() = førsteKjente til sisteKjente

    internal fun fiktiv() = perioder.flatten().singleOrNull() == EPOCH // Arbeidsperioden er gjennomført i Infotrygd

    internal fun periode(sisteDag: LocalDate) = førsteKjente til sisteDag

    internal fun kjentDag(dagen: LocalDate) {
        kjenteDager.add(dagen)
    }

    override fun compareTo(other: LocalDate) =
        førsteKjente.compareTo(other)

    operator fun contains(dato: LocalDate) =
        dato in innflytelseperioden

    operator fun contains(periode: Periode) =
        innflytelseperioden.overlapperMed(periode)

    internal fun forventerInntekt(periode: Periode): Boolean {
        return !dekkesAvArbeidsgiver(periode) && erFørsteUtbetalingsdagFørEllerLik(periode)
    }

    internal fun dekkesAvArbeidsgiver(periode: Periode): Boolean {
        val arbeidsgiversAnsvar = dagerSomarbeidsgiverUtbetaler() ?: return false
        return (periode.overlapperMed(arbeidsgiversAnsvar) && arbeidsgiversAnsvar.slutterEtter(periode.endInclusive))
    }

    private fun dagerSomarbeidsgiverUtbetaler(): Periode? {
        val heleInklHelg = arbeidsgiverperioden.justerForHelg()
        val utbetalingsperiode = utbetalingsdager.periode() ?: return heleInklHelg
        return heleInklHelg.utenDagerFør(utbetalingsperiode)
    }

    internal fun hørerTil(periode: Periode, sisteKjente: LocalDate = this.sisteKjente) =
        periode.overlapperMed(førsteKjente til sisteKjente)

    internal fun erFørsteUtbetalingsdagFørEllerLik(periode: Periode): Boolean {
        // forventer inntekt dersom vi overlapper med en utbetalingsperiode…
        if (utbetalingsdager.any { periode.overlapperMed(it) }) return true
        // …eller dersom det ikke har vært gap siden forrige utbetaling
        val forrigeUtbetaling = utbetalingsdager.lastOrNull { other -> other.endInclusive < periode.endInclusive } ?: return false
        return oppholdsdager.none { it.overlapperMed(forrigeUtbetaling.endInclusive til periode.endInclusive) }
    }

    override fun equals(other: Any?) = other is Arbeidsgiverperiode && other.førsteKjente == this.førsteKjente
    override fun hashCode() = førsteKjente.hashCode()

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

    internal fun utbetalingsdag(dato: LocalDate) = apply {
        if (!dato.erHelg()) this.utbetalingsdager.add(dato)
        kjentDag(dato)
    }

    internal fun oppholdsdag(dato: LocalDate) = apply {
        this.oppholdsdager.add(dato)
        kjentDag(dato)
    }

    internal companion object {
        private fun Periode.justerForHelg() = when (endInclusive.dayOfWeek) {
            DayOfWeek.SATURDAY -> start til endInclusive.plusDays(1)
            DayOfWeek.FRIDAY -> start til endInclusive.plusDays(2)
            else -> this
        }

        private fun MutableList<Periode>.add(dagen: LocalDate) {
            if (isNotEmpty() && last().endInclusive.plusDays(1) == dagen) {
                this[size - 1] = last().oppdaterTom(dagen)
            } else {
                add(dagen.somPeriode())
            }
        }
    }
}
