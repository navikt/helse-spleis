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
                when (val sykdomsdag = sykdomstidslinje[dag.dato]) {
                    is Dag.AndreYtelser -> PersonObserver.Utbetalingsdag(
                        dato = dag.dato,
                        type = AndreYtelser,
                        begrunnelser = listOf(sykdomsdag.tilEksternBegrunnelse())
                    )

                    is Dag.Permisjonsdag -> PersonObserver.Utbetalingsdag(dag.dato, Permisjonsdag)
                    is Dag.Feriedag -> PersonObserver.Utbetalingsdag(dag.dato, Feriedag)
                    is Dag.ArbeidIkkeGjenopptattDag -> PersonObserver.Utbetalingsdag(dag.dato, ArbeidIkkeGjenopptattDag)
                    is Dag.Arbeidsdag,
                    is Dag.ArbeidsgiverHelgedag,
                    is Dag.Arbeidsgiverdag,
                    is Dag.ForeldetSykedag,
                    is Dag.FriskHelgedag,
                    is Dag.ProblemDag,
                    is Dag.SykHelgedag,
                    is Dag.Sykedag,
                    is Dag.UkjentDag -> PersonObserver.Utbetalingsdag(dag.dato, Fridag)
                }
            }

            is Utbetalingsdag.AvvistDag -> PersonObserver.Utbetalingsdag(
                dato = dag.dato,
                type = PersonObserver.Utbetalingsdag.Dagtype.AvvistDag,
                begrunnelser = dag.begrunnelser.map {
                    PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO.fraBegrunnelse(it)
                }
            )

            is Utbetalingsdag.ForeldetDag -> PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.ForeldetDag)
            is Utbetalingsdag.UkjentDag -> PersonObserver.Utbetalingsdag(dag.dato, PersonObserver.Utbetalingsdag.Dagtype.UkjentDag)
        }
    }

    internal fun result(): List<PersonObserver.Utbetalingsdag> {
        return utbetalingsdager
    }
}
