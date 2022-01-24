package no.nav.helse.person.builders

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.serde.api.BegrunnelseDTO
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class UtbetalingsdagerBuilder(private val sykdomstidslinje: Sykdomstidslinje) : UtbetalingsdagVisitor {

    private val utbetalingsdager = mutableListOf<PersonObserver.Utbetalingsdag>()

    internal fun result() : List<PersonObserver.Utbetalingsdag> {
        return utbetalingsdager
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag))
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.NavDag))
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.NavHelgDag))
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.Arbeidsdag))
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
        val dagtype = when (sykdomstidslinje[dato]) {
            is Dag.Permisjonsdag -> PersonObserver.Utbetalingsdag.Dagtype.Permisjonsdag
            is Dag.Feriedag -> PersonObserver.Utbetalingsdag.Dagtype.Feriedag
            else -> PersonObserver.Utbetalingsdag.Dagtype.Fridag
        }
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, dagtype))
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.AvvistDag, dag.begrunnelser.map {
            BegrunnelseDTO.fraBegrunnelse(it)
        }))
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.ForeldetDag))
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.UkjentDag))
    }
}
