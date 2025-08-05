package no.nav.helse.utbetalingslinjer

import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodedagNav
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ForeldetDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Venteperiodedag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

class UtbetalingkladdBuilder(
    tidslinje: Utbetalingstidslinje,
    mottakerRefusjon: String,
    mottakerBruker: String,
    klassekodeBruker: Klassekode
) {
    // bruker samme "sak id" i OS for begge oppdragene
    // TODO: krever at Overføringer/kvitteringer inneholder fagområde, ellers
    // kan ikke meldingene mappes til riktig oppdrag
    // private val fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID())
    private val arbeidsgiveroppdragBuilder = OppdragBuilder(
        mottaker = mottakerRefusjon,
        fagområde = SykepengerRefusjon,
        klassekode = Klassekode.RefusjonIkkeOpplysningspliktig
    )
    private val personoppdragBuilder = OppdragBuilder(
        mottaker = mottakerBruker,
        fagområde = Sykepenger,
        klassekode = klassekodeBruker
    )

    init {
        tidslinje.forEach { dag ->
            when (dag) {
                is Venteperiodedag,
                is ArbeidsgiverperiodeDag -> {
                    arbeidsgiveroppdragBuilder.arbeidsgiverperiodedag(dag.dato, dag.økonomi.brukAvrundetGrad { grad -> grad })
                    personoppdragBuilder.arbeidsgiverperiodedag(dag.dato, dag.økonomi.brukAvrundetGrad { grad -> grad })
                }

                is UkjentDag,
                is Arbeidsdag,
                is AvvistDag,
                is ForeldetDag,
                is Fridag -> {
                    arbeidsgiveroppdragBuilder.ikkeBetalingsdag()
                    personoppdragBuilder.ikkeBetalingsdag()
                }

                is ArbeidsgiverperiodedagNav,
                is NavDag -> {
                    arbeidsgiveroppdragBuilder.betalingsdag(dato = dag.dato, beløp = dag.økonomi.arbeidsgiverbeløp!!.daglig.toInt(), grad = dag.økonomi.brukAvrundetGrad { grad -> grad })
                    personoppdragBuilder.betalingsdag(dato = dag.dato, beløp = dag.økonomi.personbeløp!!.daglig.toInt(), grad = dag.økonomi.brukAvrundetGrad { grad -> grad })
                }

                is NavHelgDag -> {
                    arbeidsgiveroppdragBuilder.betalingshelgedag(dag.dato, dag.økonomi.brukAvrundetGrad { grad -> grad })
                    personoppdragBuilder.betalingshelgedag(dag.dato, dag.økonomi.brukAvrundetGrad { grad -> grad })
                }
            }
        }
    }

    fun build() = Utbetalingkladd(arbeidsgiveroppdragBuilder.build(), personoppdragBuilder.build())
}
