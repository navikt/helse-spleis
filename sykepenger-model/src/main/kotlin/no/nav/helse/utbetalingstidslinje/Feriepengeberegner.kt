package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.Year

internal class Feriepengeberegner(
    private val utbetalingstidslinjer: Iterable<Utbetalingstidslinje>,
    private val alder: Alder,
    private val år: Year
) : UtbetalingsdagVisitor {
    private companion object {
        private const val MAGIC_NUMBER = 48
    }

    private val dager = mutableSetOf<LocalDate>()

    init {
        utbetalingstidslinjer.forEach { it.accept(this) }
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
        dager.add(dato)
    }

    internal fun beregn() {
        val datoer = dager
            .sorted()
            .groupBy { Year.from(it) }
            .flatMap { (_, prÅr) -> prÅr.take(MAGIC_NUMBER) }
    }
}
