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

    private val linje get() = linjer.first()

    override fun visitNavDag(dag: NavDag) {
        if (linjer.isEmpty()) return tilstand.nyLinje(dag)
        if (dag.grad == linje.grad && (linje.dagsats == 0 || linje.dagsats == dag.utbetaling))
            tilstand.betalingsdag(dag)
        else
            tilstand.nyLinje(dag)
    }

    override fun visitNavHelgDag(dag: NavHelgDag) {
        if (linjer.isEmpty() || dag.grad != linje.grad)
            tilstand.nyLinje(dag)
        else
            tilstand.helgedag(dag)
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

    private fun addLinje(dag: NavHelgDag) {
        linjer.add(0, Utbetalingslinje(dag.dato, dag.dato, 0, dag.grad))
    }

    private interface Tilstand {
        fun betalingsdag(dag: NavDag) {}
        fun helgedag(dag: NavHelgDag) {}
        fun nyLinje(dag: NavDag) {}
        fun nyLinje(dag: NavHelgDag) {}
        fun ikkeBetalingsdag() {}
    }

    private inner class Ubetalt : Tilstand {
        override fun betalingsdag(dag: NavDag) {
            addLinje(dag)
            tilstand = Betalt()
        }

        override fun nyLinje(dag: NavDag) {
            addLinje(dag)
            tilstand = Betalt()
        }

        override fun helgedag(dag: NavHelgDag) {
            addLinje(dag)
            tilstand = HelgBetalt()
        }

        override fun nyLinje(dag: NavHelgDag) {
            addLinje(dag)
            tilstand = HelgBetalt()
        }
    }

    private inner class Betalt : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = Ubetalt()
        }

        override fun betalingsdag(dag: NavDag) {
            linje.fom = dag.dato
        }

        override fun nyLinje(dag: NavDag) {
            addLinje(dag)
        }

        override fun helgedag(dag: NavHelgDag) {
            linje.fom = dag.dato
        }

        override fun nyLinje(dag: NavHelgDag) {
            addLinje(dag)
        }
    }

    private inner class HelgBetalt : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = Ubetalt()
        }

        override fun betalingsdag(dag: NavDag) {
            linje.dagsats = dag.utbetaling
            linje.fom = dag.dato
            tilstand = Betalt()
        }

        override fun nyLinje(dag: NavDag) {
            addLinje(dag)
            tilstand = Betalt()
        }

        override fun helgedag(dag: NavHelgDag) {
            linje.fom = dag.dato
        }

        override fun nyLinje(dag: NavHelgDag) {
            addLinje(dag)
        }
    }

    private inner class Avsluttet : Tilstand {}
}
