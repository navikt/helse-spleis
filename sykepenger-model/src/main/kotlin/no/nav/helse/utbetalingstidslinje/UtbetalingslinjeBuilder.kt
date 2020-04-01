package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.UtbetalingsdagVisitor

internal class UtbetalingslinjeBuilder(
    tidslinje: Utbetalingstidslinje,
    periode: Periode
) : UtbetalingsdagVisitor {
    internal val utbetalingstidslinje = tidslinje.subset(periode)
    private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()
    private var helseState: HelseState = Ubetalt()

    internal fun result(): List<Utbetalingslinje> {
        utbetalingstidslinje.accept(this)
        return utbetalingslinjer
    }

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        helseState.ikkeBetalingsdag()
    }

    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        helseState.ikkeBetalingsdag()
    }

    override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
        helseState.ikkeBetalingsdag()
    }

    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
        helseState.ikkeBetalingsdag()
    }

    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        helseState.betalingsdag(dag)
    }

    override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
        // Ignorer
    }

    override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
        helseState.ikkeBetalingsdag()
    }

    private interface HelseState {
        fun betalingsdag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag)
        fun ikkeBetalingsdag() {}
    }

    internal inner class Ubetalt : HelseState {
        override fun betalingsdag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            utbetalingslinjer.add(dag.utbetalingslinje())
            helseState = Betalt()
        }
    }

    internal inner class Betalt : HelseState {
        override fun ikkeBetalingsdag() {
            helseState = Ubetalt()
        }

        override fun betalingsdag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            if (utbetalingslinjer.last().dagsats == dag.utbetaling && utbetalingslinjer.last().grad == dag.grad)
                dag.oppdater(utbetalingslinjer.last())
            else
                utbetalingslinjer.add(dag.utbetalingslinje())
        }
    }
}
