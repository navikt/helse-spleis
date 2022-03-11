package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.contains
import no.nav.helse.hendelser.til
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.Økonomi.Companion.avgrensTilArbeidsgiverperiode

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class Utbetalingstidslinje(utbetalingsdager: List<Utbetalingsdag>) : Collection<Utbetalingsdag> by utbetalingsdager {
    private val utbetalingsdager = utbetalingsdager.toMutableList()
    private val førsteDato get() = utbetalingsdager.first().dato
    private val sisteDato get() = utbetalingsdager.last().dato

    internal constructor() : this(mutableListOf())

    init {
        check(utbetalingsdager.distinctBy { it.dato }.size == utbetalingsdager.size) {
            "Utbetalingstidslinjen består av minst én dato som pekes på av mer enn én Utbetalingsdag"
        }
    }

    internal companion object {
        internal fun periode(tidslinjer: List<Utbetalingstidslinje>) = tidslinjer
            .filter { it.isNotEmpty() }
            .map { it.periode() }
            .reduce(Periode::merge)

        internal fun avvis(
            tidslinjer: List<Utbetalingstidslinje>,
            avvistePerioder: List<Periode>,
            begrunnelser: List<Begrunnelse>
        ) = tidslinjer.forEach { it.avvis(avvistePerioder, begrunnelser) }

        internal fun avvisteDager(
            tidslinjer: List<Utbetalingstidslinje>,
            periode: Periode,
            begrunnelse: Begrunnelse
        ) = tidslinjer.flatMap { it.subset(periode) }.mapNotNull { it.erAvvistMed(begrunnelse) }

        internal fun ferdigUtbetalingstidslinje(utbetalingsdager: List<Utbetalingsdag>) = Utbetalingstidslinje(utbetalingsdager.toMutableList())
    }

    internal fun er6GBegrenset(): Boolean {
        return any { it.økonomi.er6GBegrenset() }
    }

    internal fun accept(visitor: UtbetalingsdagVisitor) {
        visitor.preVisitUtbetalingstidslinje(this)
        forEach { it.accept(visitor) }
        visitor.postVisitUtbetalingstidslinje(this)
    }

    private fun avvis(avvistePerioder: List<Periode>, begrunnelser: List<Begrunnelse>) {
        if (begrunnelser.isEmpty()) return
        utbetalingsdager.forEachIndexed { index, utbetalingsdag ->
            if (utbetalingsdag.dato in avvistePerioder) {
                utbetalingsdag.avvis(begrunnelser)?.also {
                    utbetalingsdager[index] = it
                    cached.clear()
                }
            }
        }
    }

    internal fun addArbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        add(ArbeidsgiverperiodeDag(dato, økonomi))
    }

    internal fun addNAVdag(dato: LocalDate, økonomi: Økonomi) {
        add(NavDag(dato, økonomi))
    }

    internal fun addArbeidsdag(dato: LocalDate, økonomi: Økonomi) {
        add(Arbeidsdag(dato, økonomi))
    }

    internal fun addFridag(dato: LocalDate, økonomi: Økonomi) {
        add(Fridag(dato, økonomi))
    }

    internal fun addHelg(dato: LocalDate, økonomi: Økonomi) {
        add(NavHelgDag(dato, økonomi))
    }

    internal fun addAvvistDag(dato: LocalDate, økonomi: Økonomi, begrunnelser: List<Begrunnelse>) {
        add(AvvistDag(dato, økonomi, begrunnelser))
    }

    internal fun addForeldetDag(dato: LocalDate, økonomi: Økonomi) {
        add(ForeldetDag(dato, økonomi))
    }

    private fun add(dag: Utbetalingsdag) {
        utbetalingsdager.add(dag)
        cached.clear()
    }

    internal operator fun plus(other: Utbetalingstidslinje): Utbetalingstidslinje {
        return this.plus(other) { venstre, høyre -> maxOf(venstre, høyre) }
    }

    internal fun reverse(): Utbetalingstidslinje {
        return Utbetalingstidslinje(utbetalingsdager.asReversed())
    }

    internal fun harUtbetalinger() = sykepengeperiode() != null

    internal fun plus(
        other: Utbetalingstidslinje,
        plusstrategy: (Utbetalingsdag, Utbetalingsdag) -> Utbetalingsdag
    ): Utbetalingstidslinje {
        if (other.isEmpty()) return this
        if (this.isEmpty()) return other
        return this.binde(other, plusstrategy)
    }

    private val cached = mutableListOf<Utbetalingsdag>()
    override fun iterator(): Iterator<Utbetalingsdag> {
        if (utbetalingsdager.isEmpty()) return emptyList<Utbetalingsdag>().iterator()
        val periode = if (førsteDato > sisteDato) (sisteDato til førsteDato).reversed() else førsteDato til sisteDato
        if (cached.isEmpty()) periode.forEach { cached.add(this[it]) }
        return cached.iterator()
    }

    private fun binde(
        other: Utbetalingstidslinje,
        strategy: (Utbetalingsdag, Utbetalingsdag) -> Utbetalingsdag
    ) = Utbetalingstidslinje(
        Companion.periode(listOf(this, other)).mapNotNull {
            strategy(this[it], other[it]).takeUnless { it is UkjentDag }
        }.toMutableList()
    )

    private fun trimLedendeFridager() = Utbetalingstidslinje(dropWhile { it is Fridag }.toMutableList())

    internal fun periode() = Periode(førsteDato, sisteDato)

    internal fun sykepengeperiode(): Periode? {
        val første = utbetalingsdager.firstOrNull { it is NavDag }?.dato ?: return null
        val siste = utbetalingsdager.last { it is NavDag }.dato
        return første til siste
    }

    internal fun sammenhengendeUtbetalingsperioder() =
        filter { it !is Arbeidsdag && it !is UkjentDag }
            .map(Utbetalingsdag::dato)
            .grupperSammenhengendePerioder()
            .map(::subset)
            .map(Utbetalingstidslinje::trimLedendeFridager)
            .filter { it.any { it is NavDag || it is ForeldetDag } }

    internal fun subset(periode: Periode): Utbetalingstidslinje {
        if (isEmpty()) return Utbetalingstidslinje()
        if (periode == periode()) return this
        return Utbetalingstidslinje(utbetalingsdager.filter { it.dato in periode }.toMutableList())
    }

    internal fun avgrensSisteArbeidsgiverperiode(periode: Periode): Utbetalingstidslinje {
        val tidslinje = subset(periode)
        val oppdatertPeriode = tidslinje
            .map { it.økonomi }
            .avgrensTilArbeidsgiverperiode(periode)
            ?.takeUnless { justertForArbeidsgiverperiode ->
                harUtbetalinger(justertForArbeidsgiverperiode.start til periode.start.minusDays(1))
            }
        return oppdatertPeriode?.let { subset(it) } ?: tidslinje
    }

    private fun harUtbetalinger(periode: Periode) =
        periode.asSequence().map(::get).any { it is UkjentDag || it is NavDag || it is NavHelgDag || it is ForeldetDag }

    internal fun kutt(sisteDato: LocalDate) = subset(LocalDate.MIN til sisteDato)

    internal operator fun get(dato: LocalDate) =
        utbetalingsdager.firstOrNull { it.dato == dato } ?: UkjentDag(dato, Økonomi.ikkeBetalt())

    override fun toString(): String {
        return joinToString(separator = "") {
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
        }.trim()
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

        override fun toString() = "${this.javaClass.simpleName} ($dato) ${økonomi.medData { grad, _, _ -> grad }} %"

        internal fun avvis(begrunnelser: List<Begrunnelse>) = begrunnelser
            .filter { it.skalAvvises(this) }
            .takeIf(List<*>::isNotEmpty)
            ?.let(::avvisDag)

        protected open fun avvisDag(begrunnelser: List<Begrunnelse>) = AvvistDag(dato, økonomi, begrunnelser)

        internal abstract fun accept(visitor: UtbetalingsdagVisitor)

        internal open fun gjødsle(refusjon: Refusjonshistorikk.Refusjon?) {
            økonomi.arbeidsgiverRefusjon(refusjon?.beløp(dato))
        }

        internal open fun erAvvistMed(begrunnelse: Begrunnelse): AvvistDag? = null

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
                    økonomi.medAvrundetData { _, _, _, _, _, _, arbeidsgiverbeløp, _, _ -> arbeidsgiverbeløp!! }
                }

                internal val reflectedPersonBeløp = { økonomi: Økonomi ->
                    økonomi.medAvrundetData { _, _, _, _, _, _, _, personbeløp, _ -> personbeløp!! }
                }
            }

            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
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
            internal val begrunnelser: List<Begrunnelse>
        ) : Utbetalingsdag(dato, økonomi) {
            init {
                økonomi.lås()
            }

            override val prioritet = 60
            override fun avvisDag(begrunnelser: List<Begrunnelse>) =
                AvvistDag(dato, økonomi, this.begrunnelser + begrunnelser)

            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
            internal fun navDag(): Utbetalingsdag =
                if (EgenmeldingUtenforArbeidsgiverperiode in begrunnelser) this else NavDag(dato, økonomi.låsOpp())

            override fun gjødsle(refusjon: Refusjonshistorikk.Refusjon?) {
                /* noop */
            }

            override fun erAvvistMed(begrunnelse: Begrunnelse) = takeIf { begrunnelse in begrunnelser }
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

    internal class Builder {
        private val utbetalingsdager = mutableListOf<Utbetalingsdag>()

        internal fun build() = Utbetalingstidslinje(utbetalingsdager)

        internal fun addArbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
            add(ArbeidsgiverperiodeDag(dato, økonomi))
        }

        internal fun addNAVdag(dato: LocalDate, økonomi: Økonomi) {
            add(NavDag(dato, økonomi))
        }

        internal fun addArbeidsdag(dato: LocalDate, økonomi: Økonomi) {
            add(Arbeidsdag(dato, økonomi))
        }

        internal fun addFridag(dato: LocalDate, økonomi: Økonomi) {
            add(Fridag(dato, økonomi))
        }

        internal fun addHelg(dato: LocalDate, økonomi: Økonomi) {
            add(NavHelgDag(dato, økonomi))
        }

        internal fun addUkjentDag(dato: LocalDate) =
            Økonomi.ikkeBetalt().let { økonomi ->
                if (dato.erHelg()) addFridag(dato, økonomi) else {
                    add(UkjentDag(dato, økonomi))
                }
            }

        internal fun addAvvistDag(dato: LocalDate, økonomi: Økonomi, begrunnelser: List<Begrunnelse>) {
            add(AvvistDag(dato, økonomi, begrunnelser))
        }

        internal fun addForeldetDag(dato: LocalDate, økonomi: Økonomi) {
            add(ForeldetDag(dato, økonomi))
        }

        internal fun add(dag: Utbetalingsdag) {
            utbetalingsdager.add(dag)
        }
    }
}

internal sealed class Begrunnelse() {

    internal fun skalAvvises(utbetalingsdag: Utbetalingsdag) = utbetalingsdag is AvvistDag || utbetalingsdag is NavDag

    object SykepengedagerOppbrukt : Begrunnelse()
    object SykepengedagerOppbruktOver67 : Begrunnelse()
    object MinimumInntekt : Begrunnelse()
    object MinimumInntektOver67 : Begrunnelse()
    object EgenmeldingUtenforArbeidsgiverperiode : Begrunnelse()
    object MinimumSykdomsgrad : Begrunnelse()
    object EtterDødsdato : Begrunnelse()
    object Over70 : Begrunnelse()
    object ManglerOpptjening : Begrunnelse()
    object ManglerMedlemskap : Begrunnelse()
    object NyVilkårsprøvingNødvendig : Begrunnelse()

}
