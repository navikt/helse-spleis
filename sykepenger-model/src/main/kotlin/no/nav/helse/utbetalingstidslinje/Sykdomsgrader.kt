package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import java.time.LocalDate

internal class Sykdomsgrader(tidslinjer: List<Utbetalingstidslinje>): UtbetalingsdagVisitor {

    private val grader = mutableMapOf<LocalDate, Double>()

    internal operator fun get(dato: LocalDate) = grader[dato] ?: Double.NaN

    init {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere støttes ikke (epic 7)" }
        tidslinjer.first().accept(this)
    }
    override fun visitNavDag(dag: NavDag) {
        grader[dag.dato] = dag.grad
    }
    override fun visitNavHelgDag(dag: NavHelgDag) {
        grader[dag.dato] = dag.grad
    }
}
