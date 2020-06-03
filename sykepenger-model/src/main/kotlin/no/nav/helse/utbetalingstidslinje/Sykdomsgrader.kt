package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class Sykdomsgrader(tidslinjer: List<Utbetalingstidslinje>): UtbetalingsdagVisitor {

    private val grader = mutableMapOf<LocalDate, Double>()

    internal operator fun get(dato: LocalDate) = grader[dato] ?: Double.NaN

    init {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere støttes ikke (epic 7)" }
        tidslinjer.first().accept(this)
    }
    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        aktuellDagsinntekt: Double?,
        dekningsgrunnlag: Double?,
        arbeidsgiverbeløp: Int?,
        personbeløp: Int?
    ) {
        grader[dato] = grad.toDouble()
    }
    override fun visit(
        dag: NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        grader[dag.dato] = dag.økonomi.grad().toDouble()
    }
}
