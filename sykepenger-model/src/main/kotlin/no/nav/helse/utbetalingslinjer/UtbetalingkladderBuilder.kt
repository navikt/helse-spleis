package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.utbetalingstidslinje.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi

internal class UtbetalingkladderBuilder(
    tidslinje: Utbetalingstidslinje,
    private val mottakerRefusjon: String,
    private val mottakerBruker: String
) : UtbetalingsdagVisitor {
    private val oppdrag = mutableListOf<Utbetalingkladd>()
    private var kladdBuilder: UtbetalingkladdBuilder? = null
    private var sisteArbeidsgiverdag: LocalDate? = null

    init {
        tidslinje.accept(this)
    }

    internal fun build() = ferdigstill()

    override fun visit(dag: UkjentDag, dato: LocalDate, økonomi: Økonomi) {
        ferdigstill(dato)
    }

    override fun visit(dag: ArbeidsgiverperiodeDag, dato: LocalDate, økonomi: Økonomi) {
        ferdigstill(dato)
    }

    private fun builder(dato: LocalDate) = kladdBuilder ?: resettBuilder(dato)
    private fun resettBuilder(førsteDag: LocalDate) =
        UtbetalingkladdBuilder(sisteArbeidsgiverdag ?: førsteDag, mottakerRefusjon, mottakerBruker).also {
            sisteArbeidsgiverdag = null
            kladdBuilder = it
        }

    private fun ferdigstill(dato: LocalDate? = null): List<Utbetalingkladd> {
        kladdBuilder?.build()?.also {
            oppdrag.add(it)
        }
        kladdBuilder = null
        sisteArbeidsgiverdag = dato
        return oppdrag.toList()
    }

    override fun visit(dag: NavDag, dato: LocalDate, økonomi: Økonomi) {
        builder(dato).betalingsdag(dag.beløpkilde(), dato, økonomi)
    }

    override fun visit(dag: NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
        builder(dato).betalingshelgedag(dato, økonomi)
    }

    override fun visit(dag: Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
        builder(dato).ikkeBetalingsdag(dato)
    }

    override fun visit(dag: AvvistDag, dato: LocalDate, økonomi: Økonomi) {
        builder(dato).ikkeBetalingsdag(dato)
    }

    override fun visit(dag: Fridag, dato: LocalDate, økonomi: Økonomi) {
        builder(dato).ikkeBetalingsdag(dato)
    }

    override fun visit(dag: ForeldetDag, dato: LocalDate, økonomi: Økonomi) {
        builder(dato).ikkeBetalingsdag(dato)
    }
}