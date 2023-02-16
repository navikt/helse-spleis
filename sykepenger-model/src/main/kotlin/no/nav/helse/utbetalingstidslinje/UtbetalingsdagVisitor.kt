package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Økonomi

internal interface UtbetalingsdagVisitor {
    /**
     * gjeldendePeriode vil være null om det ikke er noen utbetalingsdager her
     */
    fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje, gjeldendePeriode: Periode?) {}
    fun visit(
        dag: Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
}