package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class Utbetalingstidslinje private constructor(
    private val utbetalingsdager: MutableList<Utbetalingsdag>
) : MutableList<Utbetalingsdag> by utbetalingsdager {

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

    internal fun addArbeidsgiverperiodedag(dagsats: Int, dato: LocalDate, grad: Double = Double.NaN) {
        utbetalingsdager.add(ArbeidsgiverperiodeDag(dagsats, dato))
    }

    internal fun addNAVdag(dagsats: Int, dato: LocalDate, grad: Double) {
        utbetalingsdager.add(NavDag(dagsats, dato, grad))
    }

    internal fun addArbeidsdag(dagsats: Int, dagen: LocalDate, grad: Double = Double.NaN) {
        utbetalingsdager.add(Arbeidsdag(dagsats, dagen))
    }

    internal fun addFridag(dagen: LocalDate, grad: Double = Double.NaN) {
        utbetalingsdager.add(Fridag(0, dagen))
    }

    internal fun addHelg(dagsats: Int, dagen: LocalDate, grad: Double) {
        utbetalingsdager.add(NavHelgDag(0, dagen, grad))
    }

    private fun addUkjentDag(dagsats: Int, dagen: LocalDate, grad: Double = Double.NaN) {
        utbetalingsdager.add(UkjentDag(0, dagen))
    }

    internal fun addAvvistDag(dagsats: Int = 0, dagen: LocalDate, grad: Double, begrunnelse: Begrunnelse) {
        utbetalingsdager.add(AvvistDag(dagsats, dagen, grad, begrunnelse))
    }

    internal fun addForeldetDag(dagen: LocalDate) {
        utbetalingsdager.add(ForeldetDag(0, dagen))
    }

    private fun addUkjentDag(dato: LocalDate) =
        if (dato.erHelg()) addFridag(dato, 0.0) else addUkjentDag(0, dato, 0.0)

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

    internal fun førsteDato() = utbetalingsdager.first().dato

    private fun sisteDato(other: Utbetalingstidslinje) =
        maxOf(this.sisteDato(), other.sisteDato())

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

    internal fun kutt(sisteDato: LocalDate) = subset(førsteDato(), sisteDato)

    internal fun harUtbetalinger() = utbetalingsdager.any { it is NavDag }

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

    internal sealed class Utbetalingsdag(internal val dagsats: Int, internal val dato: LocalDate) :
        Comparable<Utbetalingsdag> {

        internal abstract val prioritet: Int

        override fun compareTo(other: Utbetalingsdag): Int {
            return this.prioritet.compareTo(other.prioritet)
        }

        abstract fun accept(visitor: UtbetalingsdagVisitor)

        internal class ArbeidsgiverperiodeDag(dagsats: Int, dato: LocalDate) : Utbetalingsdag(dagsats, dato) {
            override val prioritet = 30
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitArbeidsgiverperiodeDag(this)
        }

        internal class NavDag private constructor(
            dagsats: Int,
            dato: LocalDate,
            internal var utbetaling: Int,
            internal val grad: Double
        ) : Utbetalingsdag(dagsats, dato) {
            override val prioritet = 50

            internal constructor(dagsats: Int, dato: LocalDate, grad: Double) : this(dagsats, dato, 0, grad)

            companion object {
                internal val arbeidsgiverUtbetaling = { dag: NavDag -> dag.utbetaling }
            }

            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitNavDag(this)

            internal fun avvistDag(begrunnelse: Begrunnelse, grad: Double) =
                AvvistDag(dagsats, dato, grad, begrunnelse)
        }

        internal class NavHelgDag(dagsats: Int, dato: LocalDate, internal val grad: Double) :
            Utbetalingsdag(0, dato) {
            override val prioritet = 40
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitNavHelgDag(this)
        }

        internal class Arbeidsdag(dagsats: Int, dato: LocalDate) : Utbetalingsdag(dagsats, dato) {
            override val prioritet = 20
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitArbeidsdag(this)
        }

        internal class Fridag(dagsats: Int, dato: LocalDate) : Utbetalingsdag(dagsats, dato) {
            override val prioritet = 10
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitFridag(this)
        }

        internal class AvvistDag(
            dagsats: Int,
            dato: LocalDate,
            internal val grad: Double,
            internal val begrunnelse: Begrunnelse
        ) :
            Utbetalingsdag(dagsats, dato) {
            override val prioritet = 60
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitAvvistDag(this)
            internal fun navDag(): NavDag {
                require(!grad.isNaN()) { "Kan ikke konvertere avvist egenmeldingsdag til NavDag" }
                return NavDag(dagsats, dato, grad)
            }
        }

        internal class ForeldetDag(dagsats: Int = 0, dato: LocalDate) :
            Utbetalingsdag(dagsats, dato) {
            override val prioritet = 40 // Mellom ArbeidsgiverperiodeDag og NavDag
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitForeldetDag(this)
        }

        internal class UkjentDag(dagsats: Int, dato: LocalDate) : Utbetalingsdag(0, dato) {
            override val prioritet = 0
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visitUkjentDag(this)
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
