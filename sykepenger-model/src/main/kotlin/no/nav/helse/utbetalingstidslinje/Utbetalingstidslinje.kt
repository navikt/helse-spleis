package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.dag.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.LocalDate

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class Utbetalingstidslinje private constructor(
    private val utbetalingsdager: MutableList<Utbetalingsdag>
) {

    internal constructor() : this(mutableListOf())

    internal fun klonOgKonverterAvvistDager(): Utbetalingstidslinje =
        Utbetalingstidslinje(utbetalingsdager.map { if (it is AvvistDag && it.begrunnelse !== Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode) it.navDag() else it }
            .toMutableList())

    internal fun accept(visitor: UtbetalingsdagVisitor) {
        visitor.preVisitUtbetalingstidslinje(this)
        utbetalingsdager.forEach { it.accept(visitor) }
        visitor.postVisitUtbetalingstidslinje(this)
    }

    internal fun gjøreKortere(fom: LocalDate) = subset(fom, sisteDato())

    internal fun avvis(avvisteDatoer: List<LocalDate>, begrunnelse: Begrunnelse) {
        utbetalingsdager.forEachIndexed { index, utbetalingsdag ->
            if (utbetalingsdag is NavDag && utbetalingsdag.dato in avvisteDatoer)
                utbetalingsdager[index] = utbetalingsdag.avvistDag(begrunnelse, utbetalingsdag.grad)
        }
    }

    internal fun addArbeidsgiverperiodedag(inntekt: Double, dato: LocalDate, grad: Double = Double.NaN) {
        utbetalingsdager.add(ArbeidsgiverperiodeDag(inntekt, dato))
    }

    internal fun addNAVdag(inntekt: Double, dato: LocalDate, grad: Double) {
        utbetalingsdager.add(NavDag(inntekt, dato, grad))
    }

    internal fun addArbeidsdag(inntekt: Double, dagen: LocalDate, grad: Double = Double.NaN) {
        utbetalingsdager.add(Arbeidsdag(inntekt, dagen))
    }

    internal fun addFridag(inntekt: Double, dagen: LocalDate, grad: Double = Double.NaN) {
        utbetalingsdager.add(Fridag(inntekt, dagen))
    }

    internal fun addHelg(inntekt: Double, dagen: LocalDate, grad: Double) {
        utbetalingsdager.add(NavHelgDag(0.0, dagen, grad))
    }

    private fun addUkjentDag(inntekt: Double, dagen: LocalDate, grad: Double = Double.NaN) {
        utbetalingsdager.add(UkjentDag(0.0, dagen))
    }

    internal fun addAvvistDag(inntekt: Double = 0.0, dagen: LocalDate, grad: Double, begrunnelse: Begrunnelse) {
        utbetalingsdager.add(AvvistDag(inntekt, dagen, grad, begrunnelse))
    }

    internal fun addForeldetDag(dagen: LocalDate) {
        utbetalingsdager.add(ForeldetDag(0.0, dagen))
    }

    private fun addUkjentDag(dato: LocalDate) =
        if (dato.erHelg()) addFridag(0.0, dato, 0.0) else addUkjentDag(0.0, dato, 0.0)

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
            tidligsteDato.datesUntil(original.førsteDato()).forEach { this.addUkjentDag(it) }
            this.utbetalingsdager.addAll(original.utbetalingsdager)
            original.utbetalingsdager.last().dato.plusDays(1).datesUntil(sisteDato.plusDays(1))
                .forEach { this.addUkjentDag(it) }
        }


    private fun tidligsteDato(other: Utbetalingstidslinje) =
        minOf(this.førsteDato(), other.førsteDato())

    internal fun førsteDato() = this.utbetalingsdager.first().dato

    private fun sisteDato(other: Utbetalingstidslinje) =
        maxOf(this.sisteDato(), other.sisteDato())

    internal fun sisteDato() = this.utbetalingsdager.last().dato

    internal fun isEmpty() = utbetalingsdager.isEmpty()

    internal fun subset(fom: LocalDate, tom: LocalDate): Utbetalingstidslinje {
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


    internal sealed class Utbetalingsdag(internal val inntekt: Double, internal val dato: LocalDate) :
        Comparable<Utbetalingsdag> {

        internal abstract val prioritet: Int

        override fun compareTo(other: Utbetalingsdag): Int {
            return this.prioritet.compareTo(other.prioritet)
        }

        abstract fun accept(visitor: UtbetalingsdagVisitor)

        internal class ArbeidsgiverperiodeDag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(inntekt, dato) {
            override val prioritet = 30
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitArbeidsgiverperiodeDag(this)
        }

        internal class NavDag private constructor(
            inntekt: Double,
            dato: LocalDate,
            internal var utbetaling: Int,
            internal val grad: Double
        ) : Utbetalingsdag(inntekt, dato) {
            override val prioritet = 50

            internal constructor(inntekt: Double, dato: LocalDate, grad: Double) : this(inntekt, dato, 0, grad)

            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitNavDag(this)
            internal fun utbetalingslinje() =
                Utbetalingslinje(dato, dato, utbetaling, grad)

            internal fun oppdater(last: Utbetalingslinje) {
                last.tom = dato
            }

            internal fun avvistDag(begrunnelse: Begrunnelse, grad: Double) =
                AvvistDag(inntekt = inntekt, dato = dato, begrunnelse = begrunnelse, grad = grad)
        }

        internal class NavHelgDag(inntekt: Double, dato: LocalDate, internal val grad: Double) :
            Utbetalingsdag(0.0, dato) {
            override val prioritet = 40
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitNavHelgDag(this)
            internal fun oppdater(last: Utbetalingslinje) {
                last.tom = dato
            }
        }

        internal class Arbeidsdag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(inntekt, dato) {
            override val prioritet = 20
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitArbeidsdag(this)
        }

        internal class Fridag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(inntekt, dato) {
            override val prioritet = 10
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitFridag(this)
        }

        internal class AvvistDag(
            inntekt: Double,
            dato: LocalDate,
            internal val grad: Double,
            internal val begrunnelse: Begrunnelse
        ) :
            Utbetalingsdag(inntekt, dato) {
            override val prioritet = 60
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitAvvistDag(this)
            internal fun navDag(): NavDag {
                require(!grad.isNaN()) { "Kan ikke konvertere avvist egenmeldingsdag til NavDag" }
                return NavDag(inntekt, dato, grad)
            }
        }

        internal class ForeldetDag(inntekt: Double = 0.0, dato: LocalDate) :
            Utbetalingsdag(inntekt, dato) {
            override val prioritet = 40 // Mellom ArbeidsgiverperiodeDag og NavDag
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitForeldetDag(this)
        }

        internal class UkjentDag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(0.0, dato) {
            override val prioritet = 0
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitUkjentDag(this)
        }
    }
}

enum class Begrunnelse {
    SykepengedagerOppbrukt,
    MinimumInntekt,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad
}
