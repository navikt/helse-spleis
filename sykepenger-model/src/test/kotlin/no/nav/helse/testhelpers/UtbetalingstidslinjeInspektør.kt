package no.nav.helse.testhelpers

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

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

    override fun visit(
        dag: Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) { arbeidsdagTeller += 1 }
    override fun visit(
        dag: ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) { arbeidsgiverperiodeDagTeller += 1 }
    override fun visit(
        dag: AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) { avvistDagTeller += 1 }
    override fun visit(
        dag: Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) { fridagTeller += 1 }
    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.reflection { _, _, _, _, _, aktuellDagsinntekt, arbeidsgiverbeløp, _, _ ->
            navDagTeller += 1
            totalUtbetaling += arbeidsgiverbeløp ?: 0
            totalInntekt += aktuellDagsinntekt ?: 0.0
        }
    }
    override fun visit(
        dag: NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) { navHelgDagTeller += 1 }
    override fun visit(
        dag: UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) { ukjentDagTeller += 1 }

    internal fun totalUtbetaling() = totalUtbetaling
    internal fun totalInntekt() = totalInntekt
}
