package no.nav.helse.person.builders

import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.AndreYtelser
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.ArbeidIkkeGjenopptattDag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.Feriedag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.Fridag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype.Permisjonsdag
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class UtbetalingsdagerBuilder(private val sykdomstidslinje: Sykdomstidslinje) {

    internal fun result(utbetalingstidslinje: Utbetalingstidslinje): List<EventSubscription.Utbetalingsdag> {
        return utbetalingstidslinje.map { dag ->
            when (dag) {
                is Utbetalingsdag.Arbeidsdag -> EventSubscription.Utbetalingsdag(dag.dato, EventSubscription.Utbetalingsdag.Dagtype.Arbeidsdag, dag.økonomi.dekningsgrad.toDouble().toInt())
                is Utbetalingsdag.ArbeidsgiverperiodeDag,
                is Utbetalingsdag.ArbeidsgiverperiodedagNav -> EventSubscription.Utbetalingsdag(
                    dato = dag.dato,
                    type = EventSubscription.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag,
                    beløpTilArbeidsgiver = dag.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0,
                    beløpTilBruker = dag.økonomi.personbeløp?.dagligInt ?: 0,
                    sykdomsgrad = dag.økonomi.sykdomsgrad.toDouble().toInt(),
                    dekningsgrad = dag.økonomi.dekningsgrad.toDouble().toInt(),
                    begrunnelser = null
                )

                is Utbetalingsdag.Ventetidsdag -> EventSubscription.Utbetalingsdag(
                    dag.dato,
                    EventSubscription.Utbetalingsdag.Dagtype.Ventetidsdag,
                    beløpTilArbeidsgiver = 0,
                    beløpTilBruker = dag.økonomi.personbeløp?.dagligInt ?: 0,
                    sykdomsgrad = dag.økonomi.sykdomsgrad.toDouble().toInt(),
                    dekningsgrad = dag.økonomi.dekningsgrad.toDouble().toInt(),
                    begrunnelser = null
                )

                is Utbetalingsdag.NavDag -> EventSubscription.Utbetalingsdag(
                    dato = dag.dato,
                    type = EventSubscription.Utbetalingsdag.Dagtype.NavDag,
                    beløpTilArbeidsgiver = dag.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0,
                    beløpTilBruker = dag.økonomi.personbeløp?.dagligInt ?: 0,
                    sykdomsgrad = dag.økonomi.sykdomsgrad.toDouble().toInt(),
                    dekningsgrad = dag.økonomi.dekningsgrad.toDouble().toInt(),
                    begrunnelser = null
                )

                is Utbetalingsdag.NavHelgDag -> EventSubscription.Utbetalingsdag(
                    dato = dag.dato,
                    type = EventSubscription.Utbetalingsdag.Dagtype.NavHelgDag,
                    beløpTilArbeidsgiver = 0,
                    beløpTilBruker = 0,
                    sykdomsgrad = dag.økonomi.sykdomsgrad.toDouble().toInt(),
                    dekningsgrad = dag.økonomi.dekningsgrad.toDouble().toInt(),
                    begrunnelser = null
                )

                is Utbetalingsdag.Fridag -> {
                    when (val sykdomsdag = sykdomstidslinje[dag.dato]) {
                        is Dag.AndreYtelser -> EventSubscription.Utbetalingsdag(
                            dato = dag.dato,
                            type = AndreYtelser,
                            beløpTilArbeidsgiver = 0,
                            beløpTilBruker = 0,
                            sykdomsgrad = 0,
                            dekningsgrad = 0,
                            begrunnelser = listOf(sykdomsdag.tilEksternBegrunnelse())
                        )

                        is Dag.Permisjonsdag -> EventSubscription.Utbetalingsdag(dag.dato, Permisjonsdag, dag.økonomi.dekningsgrad.toDouble().toInt())
                        is Dag.Feriedag -> EventSubscription.Utbetalingsdag(dag.dato, Feriedag, dag.økonomi.dekningsgrad.toDouble().toInt())
                        is Dag.ArbeidIkkeGjenopptattDag -> EventSubscription.Utbetalingsdag(dag.dato, ArbeidIkkeGjenopptattDag, dag.økonomi.dekningsgrad.toDouble().toInt())
                        is Dag.Arbeidsdag,
                        is Dag.ArbeidsgiverHelgedag,
                        is Dag.Arbeidsgiverdag,
                        is Dag.ForeldetSykedag,
                        is Dag.FriskHelgedag,
                        is Dag.ProblemDag,
                        is Dag.SykHelgedag,
                        is Dag.Sykedag,
                        is Dag.UkjentDag -> EventSubscription.Utbetalingsdag(dag.dato, Fridag, dag.økonomi.dekningsgrad.toDouble().toInt())
                    }
                }

                is Utbetalingsdag.AvvistDag -> EventSubscription.Utbetalingsdag(
                    dato = dag.dato,
                    type = EventSubscription.Utbetalingsdag.Dagtype.AvvistDag,
                    beløpTilArbeidsgiver = 0,
                    beløpTilBruker = 0,
                    sykdomsgrad = 0,
                    dekningsgrad = dag.økonomi.dekningsgrad.toDouble().toInt(),
                    begrunnelser = dag.begrunnelser.map {
                        EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.fraBegrunnelse(it)
                    }
                )

                is Utbetalingsdag.ForeldetDag -> EventSubscription.Utbetalingsdag(dag.dato, EventSubscription.Utbetalingsdag.Dagtype.ForeldetDag, dag.økonomi.dekningsgrad.toDouble().toInt())
                is Utbetalingsdag.UkjentDag -> EventSubscription.Utbetalingsdag(dag.dato, EventSubscription.Utbetalingsdag.Dagtype.UkjentDag, dag.økonomi.dekningsgrad.toDouble().toInt())
            }
        }
    }
}
