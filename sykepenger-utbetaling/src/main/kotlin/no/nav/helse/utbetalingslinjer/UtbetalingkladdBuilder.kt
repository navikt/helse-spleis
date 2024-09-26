package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodedagNav
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ForeldetDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

class UtbetalingkladdBuilder(
    private val periode: Periode,
    private val tidslinje: Utbetalingstidslinje,
    private val mottakerRefusjon: String,
    private val mottakerBruker: String
) {
    // bruker samme "sak id" i OS for begge oppdragene
    // TODO: krever at Overføringer/kvitteringer inneholder fagområde, ellers
    // kan ikke meldingene mappes til riktig oppdrag
    // private val fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID())
    private var arbeidsgiveroppdragBuilder = OppdragBuilder(
        mottaker = mottakerRefusjon,
        fagområde = SykepengerRefusjon
    )
    private var personoppdragBuilder = OppdragBuilder(
        mottaker = mottakerBruker,
        fagområde = Sykepenger
    )

    init {
        tidslinje.forEach { dag ->
            when (dag) {
                is Arbeidsdag,
                is AvvistDag,
                is ForeldetDag,
                is Fridag -> {
                    arbeidsgiveroppdragBuilder.ikkeBetalingsdag()
                    personoppdragBuilder.ikkeBetalingsdag()
                }
                is ArbeidsgiverperiodedagNav,
                is NavDag -> {
                    arbeidsgiveroppdragBuilder.betalingsdag(økonomi = dag.økonomi, dato = dag.dato, grad = dag.økonomi.brukAvrundetGrad { grad -> grad })
                    personoppdragBuilder.betalingsdag(økonomi = dag.økonomi, dato = dag.dato, grad = dag.økonomi.brukAvrundetGrad { grad -> grad })
                }
                is NavHelgDag -> {
                    arbeidsgiveroppdragBuilder.betalingshelgedag(dag.dato, dag.økonomi.brukAvrundetGrad { grad -> grad })
                    personoppdragBuilder.betalingshelgedag(dag.dato, dag.økonomi.brukAvrundetGrad { grad -> grad })
                }
                is Utbetalingsdag.ArbeidsgiverperiodeDag,
                is Utbetalingsdag.UkjentDag -> { /* gjør ingenting */ }
            }
        }
    }

    fun build() = Utbetalingkladd(periode, arbeidsgiveroppdragBuilder.build(), personoppdragBuilder.build(), tidslinje)
}