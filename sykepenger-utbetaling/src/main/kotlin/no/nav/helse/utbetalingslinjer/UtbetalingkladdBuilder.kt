package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodedagNav
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ForeldetDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeVisitor
import no.nav.helse.økonomi.Økonomi

class UtbetalingkladdBuilder(
    private val periode: Periode,
    private val tidslinje: Utbetalingstidslinje,
    private val mottakerRefusjon: String,
    private val mottakerBruker: String
) : UtbetalingstidslinjeVisitor {
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
        tidslinje.accept(this)
    }

    fun build() = Utbetalingkladd(periode, arbeidsgiveroppdragBuilder.build(), personoppdragBuilder.build(), tidslinje)

    override fun visit(dag: ArbeidsgiverperiodedagNav, dato: LocalDate, økonomi: Økonomi) {
        arbeidsgiveroppdragBuilder.betalingsdag(økonomi = dag.økonomi, dato = dato, grad = økonomi.brukAvrundetGrad { grad -> grad })
        personoppdragBuilder.betalingsdag(økonomi = dag.økonomi, dato = dato, grad = økonomi.brukAvrundetGrad { grad -> grad })
    }

    override fun visit(dag: NavDag, dato: LocalDate, økonomi: Økonomi) {
        arbeidsgiveroppdragBuilder.betalingsdag(økonomi = dag.økonomi, dato = dato, grad = økonomi.brukAvrundetGrad { grad -> grad })
        personoppdragBuilder.betalingsdag(økonomi = dag.økonomi, dato = dato, grad = økonomi.brukAvrundetGrad { grad -> grad })
    }

    override fun visit(dag: NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
        arbeidsgiveroppdragBuilder.betalingshelgedag(dato, økonomi.brukAvrundetGrad { grad -> grad })
        personoppdragBuilder.betalingshelgedag(dato, økonomi.brukAvrundetGrad { grad -> grad })
    }

    override fun visit(dag: Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
        arbeidsgiveroppdragBuilder.ikkeBetalingsdag()
        personoppdragBuilder.ikkeBetalingsdag()
    }

    override fun visit(dag: AvvistDag, dato: LocalDate, økonomi: Økonomi) {
        arbeidsgiveroppdragBuilder.ikkeBetalingsdag()
        personoppdragBuilder.ikkeBetalingsdag()
    }

    override fun visit(dag: Fridag, dato: LocalDate, økonomi: Økonomi) {
        arbeidsgiveroppdragBuilder.ikkeBetalingsdag()
        personoppdragBuilder.ikkeBetalingsdag()
    }

    override fun visit(dag: ForeldetDag, dato: LocalDate, økonomi: Økonomi) {
        arbeidsgiveroppdragBuilder.ikkeBetalingsdag()
        personoppdragBuilder.ikkeBetalingsdag()
    }
}