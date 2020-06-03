package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Økonomi
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class Utbetalingstidslinje private constructor(
    private val utbetalingsdager: MutableList<Utbetalingsdag>
) : MutableList<Utbetalingsdag> by utbetalingsdager {

    internal constructor() : this(mutableListOf())

    internal companion object {
        internal fun periode(tidslinjer: List<Utbetalingstidslinje>) = Periode(
            tidslinjer.map { it.førsteDato() }.min()!!,
            tidslinjer.map { it.sisteDato() }.max()!!
        )
    }

    internal fun klonOgKonverterAvvistDager(): Utbetalingstidslinje =
        Utbetalingstidslinje(utbetalingsdager.map { if (it is AvvistDag && it.begrunnelse !== EgenmeldingUtenforArbeidsgiverperiode) it.navDag() else it }
            .toMutableList())

    internal fun accept(visitor: UtbetalingsdagVisitor) {
        visitor.preVisit(this)
        utbetalingsdager.forEach { it.accept(visitor) }
        visitor.postVisit(this)
    }

    internal fun gjøreKortere(fom: LocalDate) = subset(fom, sisteDato())

    internal fun avvis(avvisteDatoer: List<LocalDate>, begrunnelse: Begrunnelse) {
        utbetalingsdager.forEachIndexed { index, utbetalingsdag ->
            if (utbetalingsdag is NavDag && utbetalingsdag.dato in avvisteDatoer)
                utbetalingsdager[index] = utbetalingsdag.avvistDag(begrunnelse)
        }
    }

    internal fun addArbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(ArbeidsgiverperiodeDag(dato, økonomi))
    }

    internal fun addNAVdag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(NavDag(dato, økonomi))
    }

    internal fun addArbeidsdag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(Arbeidsdag(dato, økonomi))
    }

    internal fun addFridag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(Fridag(dato, økonomi))
    }

    internal fun addHelg(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(NavHelgDag(dato, økonomi))
    }

    private fun addUkjentDag(dato: LocalDate) =
        Økonomi.ikkeBetalt().inntekt(0).let { økonomi ->
            if (dato.erHelg()) addFridag(dato, økonomi) else addUkjentDag(dato, økonomi)
        }

    private fun addUkjentDag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(UkjentDag(dato, økonomi))
    }

    internal fun addAvvistDag(dato: LocalDate, økonomi: Økonomi, begrunnelse: Begrunnelse) {
        utbetalingsdager.add(AvvistDag(dato, økonomi, begrunnelse))
    }

    internal fun addForeldetDag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(ForeldetDag(dato, økonomi))
    }

    internal operator fun plus(other: Utbetalingstidslinje): Utbetalingstidslinje {
        if (other.utbetalingsdager.isEmpty()) return this
        if (this.utbetalingsdager.isEmpty()) return other
        val tidligsteDato = this.tidligsteDato(other)
        val sisteDato = this.sisteDato(other)
        return this.utvide(tidligsteDato, sisteDato).binde(other.utvide(tidligsteDato, sisteDato))
    }

    private fun binde(other: Utbetalingstidslinje) = Utbetalingstidslinje(
        this.utbetalingsdager.zip(other.utbetalingsdager)
            .map { (venstre: Utbetalingsdag, høyre: Utbetalingsdag) -> maxOf(venstre, høyre) }
            .toMutableList()
    )

    private fun utvide(tidligsteDato: LocalDate, sisteDato: LocalDate) =
        Utbetalingstidslinje().apply {
            val original = this@Utbetalingstidslinje
            tidligsteDato.datesUntil(original.førsteDato())
                .forEach { this.addUkjentDag(it) }
            this.utbetalingsdager.addAll(original.utbetalingsdager)
            original.utbetalingsdager.last().dato.plusDays(1).datesUntil(sisteDato.plusDays(1))
                .forEach { this.addUkjentDag(it) }
        }

    private fun tidligsteDato(other: Utbetalingstidslinje) =
        minOf(this.førsteDato(), other.førsteDato())

    internal fun førsteDato() = utbetalingsdager.first().dato

    private fun sisteDato(other: Utbetalingstidslinje) =
        maxOf(this.sisteDato(), other.sisteDato())

    internal fun sisteUkedag() = utbetalingsdager.last { it !is NavHelgDag }.dato
    internal fun sisteDato() = utbetalingsdager.last().dato

    internal fun førsteSykepengedag() = utbetalingsdager.firstOrNull { it is NavDag }?.dato

    internal fun sisteSykepengedag() = utbetalingsdager.lastOrNull { it is NavDag }?.dato

    internal fun sisteSykepengeperiode(): Periode? {
        val sisteDag = sisteSykepengedag() ?: return null
        var førsteDag = sisteDag
        for (challenger in kutt(sisteDag.minusDays(1)).reverse().utbetalingsdager) {
            if (challenger is NavDag || challenger is NavHelgDag || challenger is ArbeidsgiverperiodeDag) førsteDag = challenger.dato else break
        }
        return Periode(førsteDag, sisteDag)
    }

    private fun subset(fom: LocalDate, tom: LocalDate): Utbetalingstidslinje {
        return Utbetalingstidslinje(
            utbetalingsdager
                .filterNot { it.dato.isBefore(fom) || it.dato.isAfter(tom) }
                .toMutableList()
        )
    }

    internal fun subset(periode: Periode) = subset(periode.start, periode.endInclusive)

    internal fun kunArbeidsgiverdager() =
        this.utbetalingsdager.all {
            it is ArbeidsgiverperiodeDag ||
                it is Arbeidsdag ||
                it is NavHelgDag ||
                it is Fridag
        }

    internal fun reverse(): Utbetalingstidslinje {
        return Utbetalingstidslinje(utbetalingsdager.asReversed())
    }

    internal fun kutt(sisteDato: LocalDate) =
        if (utbetalingsdager.isEmpty()) this
        else subset(førsteDato(), sisteDato)

    internal fun harUtbetalinger() = utbetalingsdager.any { it is NavDag }

    internal operator fun get(dato: LocalDate) =
        if (dato in førsteDato()..sisteDato()) utbetalingsdager.first { it.dato == dato }
        else UkjentDag(dato, Økonomi.ikkeBetalt().inntekt(0))

    override fun toString(): String {
        return utbetalingsdager.joinToString(separator = "") {
            (if (it.dato.dayOfWeek == DayOfWeek.MONDAY) " " else "") +
                when (it::class) {
                    NavDag::class -> "N"
                    NavHelgDag::class -> "H"
                    Arbeidsdag::class -> "A"
                    ArbeidsgiverperiodeDag::class -> "P"
                    Fridag::class -> "F"
                    AvvistDag::class -> "X"
                    UkjentDag::class -> "U"
                    ForeldetDag::class -> "O"
                    else -> "?"
                }
        }
    }

    internal sealed class Utbetalingsdag(
        internal val dato: LocalDate,
        internal val økonomi: Økonomi
    ) :
        Comparable<Utbetalingsdag> {

        internal abstract val prioritet: Int

        override fun compareTo(other: Utbetalingsdag): Int {
            return this.prioritet.compareTo(other.prioritet)
        }

        internal abstract fun accept(visitor: UtbetalingsdagVisitor)

        internal class ArbeidsgiverperiodeDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 30
            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
        }

        internal class NavDag (
            dato: LocalDate,
            økonomi: Økonomi
        ) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 50

            companion object {
                internal val arbeidsgiverBeløp = { dag: NavDag -> Økonomi.arbeidsgiverBeløp(dag.økonomi) }
                internal val personBeløp = { dag: NavDag -> Økonomi.personBeløp(dag.økonomi) }
            }

            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)

            internal fun avvistDag(begrunnelse: Begrunnelse) =
                AvvistDag(dato, økonomi, begrunnelse)
        }

        internal class NavHelgDag(dato: LocalDate, økonomi: Økonomi) :
            Utbetalingsdag(dato, økonomi) {
            override val prioritet = 40
            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
        }

        internal class Arbeidsdag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 20
            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
        }

        internal class Fridag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 10
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        }

        internal class AvvistDag(
            dato: LocalDate,
            økonomi: Økonomi,
            internal val begrunnelse: Begrunnelse
        ) : Utbetalingsdag(dato, økonomi) {
            init {
                økonomi.lås()
            }
            override val prioritet = 60
            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
            internal fun navDag(): Utbetalingsdag = if(begrunnelse == EgenmeldingUtenforArbeidsgiverperiode) this else NavDag(dato, økonomi.låsOpp())
        }

        internal class ForeldetDag(dato: LocalDate, økonomi: Økonomi) :
            Utbetalingsdag(dato, økonomi) {
            override val prioritet = 40 // Mellom ArbeidsgiverperiodeDag og NavDag
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        }

        internal class UkjentDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 0
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        }
    }
}

internal typealias UtbetalingStrategy = (NavDag) -> Int

enum class Begrunnelse {
    SykepengedagerOppbrukt,
    MinimumInntekt,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad
}
