package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.utbetalingslinjer.BeløpkildeAdapter
import no.nav.helse.økonomi.Økonomi

internal sealed class Utbetalingsdag(
    internal val dato: LocalDate,
    internal val økonomi: Økonomi
) :
    Comparable<Utbetalingsdag> {

    internal abstract val prioritet: Int
    internal fun beløpkilde() = BeløpkildeAdapter(økonomi)
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

    internal open fun erAvvistMed(begrunnelse: Begrunnelse): AvvistDag? = null

    internal class ArbeidsgiverperiodeDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 30
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
    }

    internal class NavDag(
        dato: LocalDate,
        økonomi: Økonomi
    ) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 50
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
    }

    internal class NavHelgDag(dato: LocalDate, økonomi: Økonomi) :
        Utbetalingsdag(dato, økonomi) {
        override val prioritet = 40
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
    }

    internal class Fridag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 20
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
    }

    internal class Arbeidsdag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
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

        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)

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