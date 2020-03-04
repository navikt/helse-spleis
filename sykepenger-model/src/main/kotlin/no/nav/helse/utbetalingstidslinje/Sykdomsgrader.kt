package no.nav.helse.utbetalingstidslinje

import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.UtbetalingsdagVisitor
import java.time.LocalDate

internal class Sykdomsgrader(tidslinjer: List<Utbetalingstidslinje>): UtbetalingsdagVisitor {

    private val grader = mutableMapOf<LocalDate, Double>()


    internal operator fun get(dato: LocalDate) = grader[dato] ?: 0.0

    init {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere støttes ikke (epic 7)" }
        tidslinjer.first().accept(this)
    }
    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        grader[dag.dato] = dag.grad
    }
    override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
        grader[dag.dato] = dag.grad
    }
}
