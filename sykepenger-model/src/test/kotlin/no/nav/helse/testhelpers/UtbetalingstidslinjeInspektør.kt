package no.nav.helse.testhelpers

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*

// Collects assertable statistics for a Utbetalingstidslinje
internal class UtbetalingstidslinjeInspektør(utbetalingstidslinje: Utbetalingstidslinje):
    UtbetalingsdagVisitor {
    internal var arbeidsdagTeller = 0
    internal var arbeidsgiverperiodeDagTeller = 0
    internal var avvistDagTeller = 0
    internal var fridagTeller = 0
    internal var navDagTeller = 0
    internal var navHelgDagTeller = 0
    internal var ukjentDagTeller = 0
    internal var totalUtbetaling = 0
    internal var totalInntekt = 0.0
    internal val size get() =
        arbeidsdagTeller +
            arbeidsgiverperiodeDagTeller +
            avvistDagTeller +
            fridagTeller +
            navDagTeller +
            navHelgDagTeller +
            ukjentDagTeller

    init {
        arbeidsdagTeller = 0
        arbeidsgiverperiodeDagTeller = 0
        avvistDagTeller = 0
        fridagTeller = 0
        navDagTeller = 0
        navHelgDagTeller = 0
        ukjentDagTeller = 0
        totalUtbetaling = 0
        utbetalingstidslinje.accept(this)
    }

    override fun visitArbeidsdag(dag: Arbeidsdag) { arbeidsdagTeller += 1 }
    override fun visitArbeidsgiverperiodeDag(dag: ArbeidsgiverperiodeDag) { arbeidsgiverperiodeDagTeller += 1 }
    override fun visitAvvistDag(dag: AvvistDag) { avvistDagTeller += 1 }
    override fun visitFridag(dag: Fridag) { fridagTeller += 1 }
    override fun visitNavDag(dag: NavDag) {
        navDagTeller += 1
        totalUtbetaling += dag.utbetaling
        totalInntekt += dag.dagsats
    }
    override fun visitNavHelgDag(dag: NavHelgDag) { navHelgDagTeller += 1 }
    override fun visitUkjentDag(dag: UkjentDag) { ukjentDagTeller += 1 }
    internal fun totalUtbetaling() = totalUtbetaling
    internal fun totalInntekt() = totalInntekt
}
