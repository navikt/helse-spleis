package no.nav.helse.spleis.e2e

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import kotlin.reflect.KClass

internal class TestTidslinjeInspektør(tidslinje: Utbetalingstidslinje) :
    UtbetalingsdagVisitor {

    internal val dagtelling: MutableMap<KClass<out Utbetalingstidslinje.Utbetalingsdag>, Int> = mutableMapOf()
    internal val datoer = mutableMapOf<LocalDate, KClass<out Utbetalingstidslinje.Utbetalingsdag>>()

    init {
        tidslinje.accept(this)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dato] = Utbetalingstidslinje.Utbetalingsdag.NavDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.NavDag::class)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag::class)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AnnullertDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.AnnullertDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.AnnullertDag::class)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.UkjentDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.UkjentDag::class)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.Fridag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.Fridag::class)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.ForeldetDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.ForeldetDag::class)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class)
    }

    private fun inkrementer(klasse: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        dagtelling.compute(klasse) { _, value -> 1 + (value ?: 0) }
    }
}
