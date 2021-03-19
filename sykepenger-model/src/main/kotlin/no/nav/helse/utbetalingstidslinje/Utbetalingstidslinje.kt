package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class Utbetalingstidslinje private constructor(
    private val utbetalingsdager: MutableList<Utbetalingsdag>
) : MutableList<Utbetalingsdag> by utbetalingsdager {

    private val førsteDato get() = utbetalingsdager.first().dato
    private val sisteDato get() = utbetalingsdager.last().dato

    internal constructor() : this(mutableListOf())

    internal companion object {
        internal fun periode(tidslinjer: List<Utbetalingstidslinje>) = tidslinjer
            .filter { it.utbetalingsdager.isNotEmpty() }
            .map { it.periode() }
            .reduce(Periode::merge)

        @Suppress("SimplifiableCallChain") // avvis både avviser dager og returner true om det er avviste dager i perioden
        fun avvis(tidslinjer: List<Utbetalingstidslinje>, dager: List<LocalDate>, periode: Periode, begrunnelse: Begrunnelse) =
            tidslinjer.filter { it.avvis(dager, periode, begrunnelse) }.isNotEmpty()
    }

    internal fun er6GBegrenset(): Boolean {
        return utbetalingsdager.any {
            it.økonomi.er6GBegrenset()
        }
    }

    internal fun klonOgKonverterAvvistDager(): Utbetalingstidslinje =
        Utbetalingstidslinje(utbetalingsdager.map { if (it is AvvistDag && it.begrunnelse !== EgenmeldingUtenforArbeidsgiverperiode) it.navDag() else it }
            .toMutableList())

    internal fun accept(visitor: UtbetalingsdagVisitor) {
        visitor.preVisit(this)
        utbetalingsdager.forEach { it.accept(visitor) }
        visitor.postVisit(this)
    }

    private fun avvis(avvisteDatoer: List<LocalDate>, periode: Periode, begrunnelse: Begrunnelse): Boolean {
        var result = false
        utbetalingsdager.forEachIndexed { index, utbetalingsdag ->
            if (utbetalingsdag is NavDag && utbetalingsdag.dato in avvisteDatoer) {
                utbetalingsdager[index] = utbetalingsdag.avvistDag(begrunnelse)
                if (!result) result = utbetalingsdag.dato in periode
            }
        }
        return result
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
        Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN, skjæringstidspunkt = dato).let { økonomi ->
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
        return this.plus(other) { venstre, høyre -> maxOf(venstre, høyre) }
    }

    internal fun reverse(): Utbetalingstidslinje {
        return Utbetalingstidslinje(utbetalingsdager.asReversed())
    }

    internal fun kunArbeidsgiverdager() =
        this.utbetalingsdager.all {
            it is ArbeidsgiverperiodeDag ||
                it is Arbeidsdag ||
                it is NavHelgDag ||
                it is Fridag
        }

    internal fun kunFridager() =
        this.utbetalingsdager.all {
                it is NavHelgDag ||
                it is Fridag
        }

    internal fun harUtbetalinger() = sykepengeperiode() != null

    internal fun plus(
        other: Utbetalingstidslinje,
        plusstrategy: (Utbetalingsdag, Utbetalingsdag) -> Utbetalingsdag
    ): Utbetalingstidslinje {
        if (other.isEmpty()) return this
        if (this.isEmpty()) return other
        val tidligsteDato = this.tidligsteDato(other)
        val sisteDato = this.sisteDato(other)
        return this.utvide(tidligsteDato, sisteDato).binde(other.utvide(tidligsteDato, sisteDato), plusstrategy)
    }

    private fun binde(
        other: Utbetalingstidslinje,
        strategy: (Utbetalingsdag, Utbetalingsdag) -> Utbetalingsdag
    ) = Utbetalingstidslinje(
        this.utbetalingsdager.zip(other.utbetalingsdager, strategy).toMutableList()
    ).trim()

    private fun trim(): Utbetalingstidslinje {
        val første = firstOrNull { it !is UkjentDag }?.dato ?: return Utbetalingstidslinje()
        return subset(første til last { it !is UkjentDag }.dato)
    }

    private fun utvide(tidligsteDato: LocalDate, sisteDato: LocalDate) =
        Utbetalingstidslinje().apply {
            val original = this@Utbetalingstidslinje
            tidligsteDato.datesUntil(original.førsteDato)
                .forEach { this.addUkjentDag(it) }
            this.utbetalingsdager.addAll(original.utbetalingsdager)
            original.sisteDato.plusDays(1).datesUntil(sisteDato.plusDays(1))
                .forEach { this.addUkjentDag(it) }
        }

    private fun tidligsteDato(other: Utbetalingstidslinje) =
        minOf(this.førsteDato, other.førsteDato)

    private fun sisteDato(other: Utbetalingstidslinje) =
        maxOf(this.sisteDato, other.sisteDato)

    internal fun sisteUkedag() = utbetalingsdager.last { it !is NavHelgDag }.dato

    internal fun periode() = Periode(førsteDato, sisteDato)
    internal fun sykepengeperiode(): Periode? {
        val første = utbetalingsdager.firstOrNull { it is NavDag }?.dato ?: return null
        val siste = utbetalingsdager.last { it is NavDag }.dato
        return første til siste
    }

    internal fun subset(periode: Periode): Utbetalingstidslinje {
        if (isEmpty()) return Utbetalingstidslinje()
        if (periode == periode()) return this
        return Utbetalingstidslinje(utbetalingsdager.filter { it.dato in periode }.toMutableList())
    }
    internal fun kutt(sisteDato: LocalDate) = subset(LocalDate.MIN til sisteDato)

    internal operator fun get(dato: LocalDate) =
        if (isEmpty() || dato !in periode()) UkjentDag(dato, Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN, skjæringstidspunkt = dato))
        else utbetalingsdager.first { it.dato == dato }

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

        internal class NavDag(
            dato: LocalDate,
            økonomi: Økonomi
        ) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 50

            companion object {
                internal val reflectedArbeidsgiverBeløp = { økonomi: Økonomi ->
                    økonomi.reflectionRounded { _, _, _, _, arbeidsgiverbeløp, _, _ -> arbeidsgiverbeløp!! }
                }

                internal val reflectedPersonBeløp = { økonomi: Økonomi ->
                    økonomi.reflectionRounded { _, _, _, _, _, personBeløp, _ -> personBeløp!! }
                }
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
            internal fun navDag(): Utbetalingsdag =
                if (begrunnelse == EgenmeldingUtenforArbeidsgiverperiode) this else NavDag(dato, økonomi.låsOpp())
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

enum class Begrunnelse {
    SykepengedagerOppbrukt,
    MinimumInntekt,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad,
    EtterDødsdato
}
