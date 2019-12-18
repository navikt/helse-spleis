package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class Utbetalingstidslinje {

    private lateinit var visitor: Utbetalingsgrense

    constructor() {
        utbetalingsdager = mutableListOf()
    }

    internal fun maksdato() = visitor.maksdato()
    internal fun antallGjenståendeSykedager() = visitor.antallGjenståendeSykedager()

    private constructor(utbetalingsdager: List<Utbetalingsdag>) {
        this.utbetalingsdager = utbetalingsdager.toMutableList()
    }

    private val utbetalingsdager: MutableList<Utbetalingsdag>

    internal fun accept(visitor: UtbetalingsdagVisitor) {
        utbetalingsdager.forEach { it.accept(visitor) }
    }

    internal fun utbetalingslinjer(others: List<Utbetalingstidslinje>, alderRegler: AlderRegler, førsteDag: LocalDate, sisteDag: LocalDate) =
        this
            .avgrens(others, alderRegler)
            .filterByMinimumInntekt(others, alderRegler)
            .reduserAvSykdomsgrad(others)
            .subset(førsteDag, sisteDag)
            .utbetalingslinjer()

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


    private fun filterByMinimumInntekt(
        others: List<Utbetalingstidslinje>,
        alderRegler: AlderRegler
    ) = this

    private fun reduserAvSykdomsgrad(others: List<Utbetalingstidslinje>) = this

    private fun subset(fom: LocalDate, tom: LocalDate) : Utbetalingstidslinje {
        return Utbetalingstidslinje(utbetalingsdager.filterNot { it.dato.isBefore(fom) || it.dato.isAfter(tom) })
    }

    private fun utbetalingslinjer() = UtbetalingslinjeBuilder(this).result()

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

    internal interface UtbetalingsdagVisitor {
        fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
        fun visitArbeidsgiverperiodeDag(dag: Utbetalingsdag.ArbeidsgiverperiodeDag) {}
        fun visitNavDag(dag: Utbetalingsdag.NavDag) {}
        fun visitNavHelgDag(dag: Utbetalingsdag.NavHelgDag) {}
        fun visitArbeidsdag(dag: Utbetalingsdag.Arbeidsdag) {}
        fun visitFridag(dag: Utbetalingsdag.Fridag) {}
        fun visitAvvistDag(dag: Utbetalingsdag.AvvistDag) {}
        fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
    }

    internal sealed class Utbetalingsdag(internal val inntekt: Double, internal val dato: LocalDate) {

        companion object {
            internal fun subset(liste: List<Utbetalingsdag>, fom: LocalDate, tom: LocalDate) = liste.filter { it.dato.isAfter(fom.minusDays(1)) && it.dato.isBefore(tom.plusDays(1)) }
        }

        abstract fun accept(visitor: UtbetalingsdagVisitor)

        internal class ArbeidsgiverperiodeDag(inntekt: Double, dato: LocalDate) :
            Utbetalingsdag(inntekt, dato) {

            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitArbeidsgiverperiodeDag(this)
        }

        internal class NavDag(inntekt: Double, dato: LocalDate) :
            Utbetalingsdag(inntekt, dato) {

            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitNavDag(this)

            fun utbetalingslinje() = Utbetalingslinje(dato, dato, inntekt.roundToInt())
            fun oppdater(last: Utbetalingslinje) {
                last.tom = dato
            }
        }

        internal class NavHelgDag(inntekt: Double, dato: LocalDate) :
            Utbetalingsdag(0.0, dato) {

            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitNavHelgDag(this)

            fun oppdater(last: Utbetalingslinje) {
                last.tom = dato
            }
        }

        internal class Arbeidsdag(inntekt: Double, dagen: LocalDate) :
            Utbetalingsdag(inntekt, dagen) {

            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitArbeidsdag(this)

        }

        internal class Fridag(inntekt: Double, dato: LocalDate) :
            Utbetalingsdag(inntekt, dato) {

            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitFridag(this)
        }

        internal class AvvistDag(internal val dag: LocalDate, internal val begrunnelse: Begrunnelse) :
            Utbetalingsdag(0.0, dag) {

            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitAvvistDag(this)

        }
    }

}

enum class Begrunnelse {
    SykepengedagerOppbrukt
}
