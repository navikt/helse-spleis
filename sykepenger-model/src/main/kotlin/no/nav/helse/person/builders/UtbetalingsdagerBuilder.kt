package no.nav.helse.person.builders

import java.time.LocalDate
import no.nav.helse.Toggle
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.AndreYtelser
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.ArbeidIkkeGjenopptattDag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.Feriedag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.Fridag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.Permisjonsdag
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeVisitor
import no.nav.helse.økonomi.Økonomi

internal class UtbetalingsdagerBuilder(private val sykdomstidslinje: Sykdomstidslinje) : UtbetalingstidslinjeVisitor {

    private val utbetalingsdager = mutableListOf<PersonObserver.Utbetalingsdag>()

    internal fun result() : List<PersonObserver.Utbetalingsdag> {
        return utbetalingsdager
    }

    override fun visit(dag: Utbetalingsdag.ArbeidsgiverperiodeDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag))
    }

    override fun visit(dag: Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.NavDag))
    }

    override fun visit(dag: Utbetalingsdag.ArbeidsgiverperiodedagNav, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag))
    }

    override fun visit(dag: Utbetalingsdag.NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.NavHelgDag))
    }

    override fun visit(dag: Utbetalingsdag.Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.Arbeidsdag))
    }

    override fun visit(dag: Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
        val (dagtype, begrunnelse) = when (sykdomstidslinje[dato]) {
            is Dag.Permisjonsdag -> Permisjonsdag to null
            is Dag.Feriedag -> Feriedag to null
            is Dag.ArbeidIkkeGjenopptattDag -> ArbeidIkkeGjenopptattDag to null
            is Dag.AndreYtelser -> if (Toggle.AndreYtelserUnderveis.enabled) AndreYtelser to eksternBegrunnelse(sykdomstidslinje[dato])?.let { listOf(it) } else Fridag to null
            else -> Fridag to null
        }
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, dagtype, begrunnelse))
    }

    private fun eksternBegrunnelse(dag: Dag): PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO? {
        return when(dag) {
            is Dag.AndreYtelser -> dag.tilEksternBegrunnelse()
            else -> null
        }
    }

    override fun visit(dag: Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.AvvistDag, dag.begrunnelser.map {
            PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO.fraBegrunnelse(it)
        }))
    }

    override fun visit(dag: Utbetalingsdag.ForeldetDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.ForeldetDag))
    }

    override fun visit(dag: Utbetalingsdag.UkjentDag, dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(PersonObserver.Utbetalingsdag(dato, PersonObserver.Utbetalingsdag.Dagtype.UkjentDag))
    }
}
