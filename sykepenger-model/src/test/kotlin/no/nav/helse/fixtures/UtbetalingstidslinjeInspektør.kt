package no.nav.helse.fixtures

import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*

// Collects assertable statistics for a Utbetalingstidslinje
internal class UtbetalingstidslinjeInspektør(private val utbetalingstidslinje: Utbetalingstidslinje): Utbetalingstidslinje.UtbetalingsdagVisitor {
    internal var arbeidsgiverperiodeDagTeller = 0
    internal var navDagTeller = 0
    internal var navHelgDagTeller = 0
    internal var arbeidsdagTeller = 0
    internal var fridagTeller = 0
    internal var avvistDagTeller = 0
    internal val size get() = arbeidsgiverperiodeDagTeller + navDagTeller + navHelgDagTeller + arbeidsdagTeller + fridagTeller + avvistDagTeller

    internal fun result(): UtbetalingstidslinjeInspektør {
        utbetalingstidslinje.accept(this)
        return this
    }

    override fun visitArbeidsgiverperiodeDag(dag: ArbeidsgiverperiodeDag) { arbeidsgiverperiodeDagTeller += 1 }
    override fun visitNavDag(dag: NavDag) { navDagTeller += 1 }
    override fun visitNavHelgDag(dag: NavHelgDag) { navHelgDagTeller += 1 }
    override fun visitArbeidsdag(dag: Arbeidsdag) { arbeidsdagTeller += 1 }
    override fun visitFridag(dag: Fridag) { fridagTeller += 1 }
    override fun visitAvvistDag(dag: AvvistDag) { avvistDagTeller += 1 }
}
