package no.nav.helse.person.builders

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.AndreYtelser
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.ArbeidIkkeGjenopptattDag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.Feriedag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.Fridag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.Permisjonsdag
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class UtbetalingsdagerBuilder(private val sykdomstidslinje: Sykdomstidslinje, utbetalingstidslinje: Utbetalingstidslinje) {

    private val utbetalingsdager = utbetalingstidslinje.map { dag ->
        when (dag) {
            is Utbetalingsdag.Arbeidsdag -> PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.Arbeidsdag)
            is Utbetalingsdag.ArbeidsgiverperiodeDag,
            is Utbetalingsdag.ArbeidsgiverperiodedagNav -> PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag)
            is Utbetalingsdag.NavDag -> PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.NavDag)
            is Utbetalingsdag.NavHelgDag -> PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.NavHelgDag)
            is Utbetalingsdag.Fridag -> {
                val (dagtype, begrunnelse) = when (sykdomstidslinje[dag.dato]) {
                    is Dag.Permisjonsdag -> Permisjonsdag to null
                    is Dag.Feriedag -> Feriedag to null
                    is Dag.ArbeidIkkeGjenopptattDag -> ArbeidIkkeGjenopptattDag to null
                    is Dag.AndreYtelser -> AndreYtelser to eksternBegrunnelse(sykdomstidslinje[dag.dato])?.let { listOf(it) }
                    is Dag.Arbeidsdag,
                    is Dag.ArbeidsgiverHelgedag,
                    is Dag.Arbeidsgiverdag,
                    is Dag.ForeldetSykedag,
                    is Dag.FriskHelgedag,
                    is Dag.ProblemDag,
                    is Dag.SykHelgedag,
                    is Dag.Sykedag,
                    is Dag.SykedagNav,
                    is Dag.UkjentDag -> Fridag to null
                }
                PersonObserver.Utbetalingsdag(dag.dato, dagtype, begrunnelse)
            }
            is Utbetalingsdag.AvvistDag -> {
                PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.AvvistDag, dag.begrunnelser.map {
                    PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO.fraBegrunnelse(it)
                })
            }
            is Utbetalingsdag.ForeldetDag -> PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.ForeldetDag)
            is Utbetalingsdag.UkjentDag -> PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.UkjentDag)
        }
    }

    internal fun result() : List<PersonObserver.Utbetalingsdag> {
        return utbetalingsdager
    }

    private fun eksternBegrunnelse(dag: Dag): PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO? {
        return when(dag) {
            is Dag.AndreYtelser -> dag.tilEksternBegrunnelse()
            else -> null
        }
    }
}
