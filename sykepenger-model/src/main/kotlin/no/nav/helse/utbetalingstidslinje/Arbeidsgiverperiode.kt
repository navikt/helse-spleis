package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.sykdomstidslinje.erRettFør
import java.time.DayOfWeek
import java.time.LocalDate

internal class Arbeidsgiverperiode private constructor(private val perioder: List<Periode>, private var førsteUtbetalingsdag: LocalDate?) : Iterable<LocalDate>, Comparable<LocalDate> {
    constructor(perioder: List<Periode>) : this(perioder, null)

    init {
        check(perioder.isNotEmpty() || førsteUtbetalingsdag != null) {
            "Enten må arbeidsgiverperioden være oppgitt eller så må første utbetalingsdag være oppgitt"
        }
    }

    private val kjenteDager = mutableListOf<Periode>().also { kjenteDager -> førsteUtbetalingsdag?.let { kjenteDager.add(it.somPeriode()) } }
    private val første = requireNotNull(perioder.firstOrNull()?.start ?: førsteUtbetalingsdag)
    private val siste = requireNotNull(perioder.lastOrNull()?.endInclusive ?: førsteUtbetalingsdag)
    private val hele = første til siste
    private val sisteKjente get() = kjenteDager.lastOrNull()?.endInclusive?.let { maxOf(it, siste) } ?: siste

    internal fun fiktiv() = perioder.isEmpty()

    internal fun kjentDag(dagen: LocalDate) {
        if (kjenteDager.isNotEmpty() && kjenteDager.last().endInclusive.plusDays(1) == dagen) {
            kjenteDager[kjenteDager.size - 1] = kjenteDager.last().oppdaterTom(dagen)
        } else {
            kjenteDager.add(dagen.somPeriode())
        }
    }

    override fun compareTo(other: LocalDate) =
        første.compareTo(other)

    operator fun contains(dato: LocalDate) =
        dato in hele

    operator fun contains(periode: Periode) =
        periode.overlapperMed(første til sisteKjente)

    internal fun dekker(periode: Periode): Boolean {
        val heleInklHelg = hele.justerForHelg()
        return (periode.overlapperMed(heleInklHelg) && heleInklHelg.slutterEtter(periode.endInclusive))
    }

    internal fun hørerTil(periode: Periode, sisteKjente: LocalDate = this.sisteKjente) =
        periode.overlapperMed(første til sisteKjente)

    internal fun sammenlign(other: List<Periode>): Boolean {
        if (fiktiv()) return true
        val otherSiste = other.lastOrNull()?.endInclusive ?: return false
        val thisSiste = this.perioder.last().endInclusive
        return otherSiste == thisSiste || (thisSiste.erHelg() && otherSiste.erRettFør(thisSiste)) || (otherSiste.erHelg() && thisSiste.erRettFør(otherSiste))
    }

    internal fun harBetalt(dato: LocalDate) = førsteUtbetalingsdag?.let { dato >= it } ?: false

    override fun equals(other: Any?) = other is Arbeidsgiverperiode && other.første == this.første
    override fun hashCode() = første.hashCode()

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
        if (førsteUtbetalingsdag != null) return@apply kjentDag(dato)
        this.førsteUtbetalingsdag = dato
    }

    internal companion object {
        internal fun fiktiv(førsteUtbetalingsdag: LocalDate) = Arbeidsgiverperiode(emptyList(), førsteUtbetalingsdag)

        internal fun List<Arbeidsgiverperiode>.finn(periode: Periode) = firstOrNull { arbeidsgiverperiode ->
            periode in arbeidsgiverperiode
        }

        private fun Periode.justerForHelg() = when (endInclusive.dayOfWeek) {
            DayOfWeek.SATURDAY -> start til endInclusive.plusDays(1)
            DayOfWeek.FRIDAY -> start til endInclusive.plusDays(2)
            else -> this
        }
    }
}
