package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import java.time.LocalDate

internal class SpennBuilder(
    tidslinje: Utbetalingstidslinje,
    sisteDato: LocalDate = tidslinje.sisteDato()
) : UtbetalingsdagVisitor {
    private val linjer = mutableListOf<Utbetalingslinje>()
    private var tilstand: Tilstand = Ubetalt()

    init {
        tidslinje.kutt(sisteDato).reverse().accept(this)
    }

    internal fun result(): List<Utbetalingslinje> {
        return linjer
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        println(tidslinje)
    }

    override fun visitNavDag(dag: NavDag) {
        tilstand.betalingsdag(dag)
    }

    override fun visitNavHelgDag(dag: NavHelgDag) {
        // ignorer
    }

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        tilstand = Avsluttet()
    }

    override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
        tilstand.ikkeBetalingsdag()
    }

    private fun addLinje(dag: NavDag) {
        linjer.add(0, Utbetalingslinje(dag.dato, dag.dato, dag.utbetaling, dag.grad))
    }

    private interface Tilstand {
        fun betalingsdag(dag: NavDag) {}
        fun ikkeBetalingsdag() {}
    }

    private inner class Ubetalt : Tilstand {
        override fun betalingsdag(dag: NavDag) {
            linjer.add(0, Utbetalingslinje(dag.dato, dag.dato, dag.utbetaling, dag.grad))
            tilstand = Betalt()
        }
    }

    private inner class Betalt : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = Ubetalt()
        }

        override fun betalingsdag(dag: NavDag) {
            if (linjer.first().dagsats == dag.utbetaling && linjer.first().grad == dag.grad)
                linjer.first().fom = dag.dato
            else
                addLinje(dag)
        }
    }

    private inner class Avsluttet : Tilstand {}
}
