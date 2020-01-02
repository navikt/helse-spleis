package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.helse.sykdomstidslinje.dag.erHelg
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class Utbetalingstidslinje internal constructor() {

    private lateinit var visitor: Utbetalingsgrense
    private val utbetalingsdager = mutableListOf<Utbetalingsdag>()

    private constructor(utbetalingsdager: List<Utbetalingsdag>): this() {
        this.utbetalingsdager.addAll(utbetalingsdager)
    }

    internal fun maksdato() = visitor.maksdato()

    internal fun antallGjenståendeSykedager() = visitor.antallGjenståendeSykedager()

    internal fun accept(visitor: UtbetalingsdagVisitor) {
        visitor.preVisitUtbetalingstidslinje(this)
        utbetalingsdager.forEach { it.accept(visitor) }
        visitor.postVisitUtbetalingstidslinje(this)
    }

    internal fun gjøreKortere(fom: LocalDate) = subset(fom, utbetalingsdager.last().dato)

    internal fun utbetalingslinjer(others: List<Utbetalingstidslinje>, alderRegler: AlderRegler, førsteDag: LocalDate, sisteDag: LocalDate) =
        this
            .avgrens(others, alderRegler)
            .filterByMinimumInntekt(others, alderRegler)
            .reduserAvSykdomsgrad(others)
            .subset(førsteDag, sisteDag)
            .utbetalingslinjer()

    internal fun addArbeidsgiverperiodedag(inntekt: Double, dato: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.ArbeidsgiverperiodeDag(inntekt, dato))
    }

    internal fun addNAVdag(inntekt: Double, dato: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.NavDag(inntekt, dato))
    }

    internal fun addArbeidsdag(inntekt: Double, dagen: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.Arbeidsdag(inntekt, dagen))
    }

    internal fun addFridag(inntekt: Double, dagen: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.Fridag(inntekt, dagen))
    }

    internal fun addHelg(inntekt: Double, dagen: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.NavHelgDag(0.0, dagen))
    }

    private fun addUkjentDag(inntekt: Double, dagen: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.UkjentDag(0.0, dagen))
    }

    private fun avgrens(others: List<Utbetalingstidslinje>, alderRegler: AlderRegler) : Utbetalingstidslinje {
        visitor = Utbetalingsgrense(alderRegler)
        this.merge(others).accept(visitor)

        return Utbetalingstidslinje(this.utbetalingsdager
            .map { utbetalingdag -> visitor.ubetalteDager().firstOrNull(){ it.dag.isEqual(utbetalingdag.dato) } ?: utbetalingdag })
    }

    private fun merge(others: List<Utbetalingstidslinje>): Utbetalingstidslinje {
        require(others.isEmpty()) { "Hello future programmer, you need to implement support for multiple employers" }
        return this
    }

    operator internal fun plus(other: Utbetalingstidslinje): Utbetalingstidslinje {
        val tidligsteDato = this.tidligsteDato(other)
        val sisteDato = this.sisteDato(other)
        return this.utvide(tidligsteDato, sisteDato).binde(other.utvide(tidligsteDato, sisteDato))
    }

    private fun binde(other: Utbetalingstidslinje) = Utbetalingstidslinje(
        this.utbetalingsdager.zip(other.utbetalingsdager)
            .map { (venstre: Utbetalingsdag, høyre: Utbetalingsdag) -> maxOf(venstre, høyre) }
    )

    private fun utvide(tidligsteDato: LocalDate, sisteDato: LocalDate) =
        Utbetalingstidslinje().apply {
            val original = this@Utbetalingstidslinje
            tidligsteDato.datesUntil(original.utbetalingsdager.first().dato).forEach { this.addUkjentDag(it) }
            this.utbetalingsdager.addAll(original.utbetalingsdager)
            original.utbetalingsdager.last().dato.plusDays(1).datesUntil(sisteDato.plusDays(1)).forEach { this.addUkjentDag(it) }
        }

    private fun addUkjentDag(dato: LocalDate) = if (dato.erHelg()) addFridag(0.0, dato) else addUkjentDag(0.0, dato)

    private fun tidligsteDato(other: Utbetalingstidslinje) =
        minOf(this.utbetalingsdager.first().dato, other.utbetalingsdager.first().dato)

    private fun sisteDato(other: Utbetalingstidslinje) =
        maxOf(this.utbetalingsdager.last().dato, other.utbetalingsdager.last().dato)

    private fun filterByMinimumInntekt(
        others: List<Utbetalingstidslinje>,
        alderRegler: AlderRegler
    ) = this

    private fun reduserAvSykdomsgrad(others: List<Utbetalingstidslinje>) = this

    private fun subset(fom: LocalDate, tom: LocalDate) : Utbetalingstidslinje {
        return Utbetalingstidslinje(utbetalingsdager.filterNot { it.dato.isBefore(fom) || it.dato.isAfter(tom) })
    }

    private fun utbetalingslinjer() = UtbetalingslinjeBuilder(this).result()

    internal interface UtbetalingsdagVisitor {
        fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
        fun visitArbeidsgiverperiodeDag(dag: Utbetalingsdag.ArbeidsgiverperiodeDag) {}
        fun visitNavDag(dag: Utbetalingsdag.NavDag) {}
        fun visitNavHelgDag(dag: Utbetalingsdag.NavHelgDag) {}
        fun visitArbeidsdag(dag: Utbetalingsdag.Arbeidsdag) {}
        fun visitFridag(dag: Utbetalingsdag.Fridag) {}
        fun visitAvvistDag(dag: Utbetalingsdag.AvvistDag) {}
        fun visitUkjentDag(dag: Utbetalingsdag.UkjentDag) {}
        fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
    }

    internal sealed class Utbetalingsdag(internal val inntekt: Double, internal val dato: LocalDate)
        : Comparable<Utbetalingsdag> {

        companion object {
            internal fun subset(liste: List<Utbetalingsdag>, fom: LocalDate, tom: LocalDate) = liste.filter { it.dato.isAfter(fom.minusDays(1)) && it.dato.isBefore(tom.plusDays(1)) }
        }

        internal abstract val prioritet: Int

        override fun compareTo(other: Utbetalingsdag): Int {
            return this.prioritet.compareTo(other.prioritet)
        }

        abstract fun accept(visitor: UtbetalingsdagVisitor)

        internal class ArbeidsgiverperiodeDag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(inntekt, dato) {
            override val prioritet = 30
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitArbeidsgiverperiodeDag(this)
        }

        internal class NavDag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(inntekt, dato) {
            override val prioritet = 50
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitNavDag(this)
            internal fun utbetalingslinje() = Utbetalingslinje(dato, dato, inntekt.roundToInt())
            internal fun oppdater(last: Utbetalingslinje) { last.tom = dato }
        }

        internal class NavHelgDag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(0.0, dato) {
            override val prioritet = 40
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitNavHelgDag(this)
            fun oppdater(last: Utbetalingslinje) { last.tom = dato }
        }

        internal class Arbeidsdag(inntekt: Double, dagen: LocalDate) : Utbetalingsdag(inntekt, dagen) {
            override val prioritet = 20
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitArbeidsdag(this)
        }

        internal class Fridag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(inntekt, dato) {
            override val prioritet = 10
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitFridag(this)
        }

        internal class AvvistDag(internal val dag: LocalDate, internal val begrunnelse: Begrunnelse) :
            Utbetalingsdag(0.0, dag) {
            override val prioritet = 60
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitAvvistDag(this)
        }

        internal class UkjentDag(inntekt: Double, dato: LocalDate) : Utbetalingsdag(0.0, dato) {
            override val prioritet = 0
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitUkjentDag(this)
        }
    }

}

enum class Begrunnelse {
    SykepengedagerOppbrukt
}
