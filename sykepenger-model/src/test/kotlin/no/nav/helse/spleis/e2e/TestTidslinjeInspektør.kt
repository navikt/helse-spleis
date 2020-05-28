package no.nav.helse.spleis.e2e

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import kotlin.reflect.KClass

internal class TestTidslinjeInspektør(tidslinje: Utbetalingstidslinje) :
    UtbetalingsdagVisitor {

    internal val dagtelling: MutableMap<KClass<out Utbetalingstidslinje.Utbetalingsdag>, Int> = mutableMapOf()
    internal val datoer = mutableMapOf<LocalDate, KClass<out Utbetalingstidslinje.Utbetalingsdag>>()

    init {
        tidslinje.accept(this)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.NavDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.NavDag::class)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag::class)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.UkjentDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.UkjentDag::class)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.Fridag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.Fridag::class)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.ForeldetDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.ForeldetDag::class)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
        datoer[dag.dato] = Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class
        inkrementer(Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class)
    }

    private fun inkrementer(klasse: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        dagtelling.compute(klasse) { _, value -> 1 + (value ?: 0) }
    }
}
