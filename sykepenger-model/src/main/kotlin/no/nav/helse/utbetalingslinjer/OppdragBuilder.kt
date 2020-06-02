package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.UtbetalingStrategy
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.*

internal class OppdragBuilder(
    tidslinje: Utbetalingstidslinje,
    private val orgnummer: String,
    private val fagområde: Fagområde,
    sisteDato: LocalDate = tidslinje.sisteDato(),
    private val dagStrategy: UtbetalingStrategy = NavDag.arbeidsgiverUtbetaling
) : UtbetalingsdagVisitor {
    private val arbeisdsgiverLinjer = mutableListOf<Utbetalingslinje>()
    private var tilstand: Tilstand = MellomLinjer()
    private val fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID())
    private var sisteArbeidsgiverdag: LocalDate? = null

    init {
        tidslinje.kutt(sisteDato).reverse().accept(this)
    }

    internal fun result(): Oppdrag {
        arbeisdsgiverLinjer.removeAll { it.dagsats == 0 }
        arbeisdsgiverLinjer.zipWithNext { a, b -> b.linkTo(a) }
        arbeisdsgiverLinjer.firstOrNull()?.refFagsystemId = null
        return Oppdrag(orgnummer, fagområde, arbeisdsgiverLinjer, fagsystemId, sisteArbeidsgiverdag)
    }

    private val linje get() = arbeisdsgiverLinjer.first()

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (arbeisdsgiverLinjer.isEmpty()) return tilstand.nyLinje(dag)
        if (dag.økonomi.grad().toDouble() == linje.grad && (linje.dagsats == 0 || linje.dagsats == dagStrategy(dag)))
            tilstand.betalingsdag(dag)
        else
            tilstand.nyLinje(dag)
    }

    override fun visit(
        dag: NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (arbeisdsgiverLinjer.isEmpty() || dag.økonomi.grad().toDouble() != linje.grad)
            tilstand.nyLinje(dag)
        else
            tilstand.helgedag(dag)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        sisteArbeidsgiverdag?.let { sisteArbeidsgiverdag = dag.dato }
        tilstand = Avsluttet
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.ikkeBetalingsdag()
    }

    private fun addLinje(dag: NavDag) {
        arbeisdsgiverLinjer.add(
            0,
            Utbetalingslinje(
                dag.dato,
                dag.dato,
                dagStrategy(dag),
                dag.økonomi.dekningsgrunnlag().toInt(),
                dag.økonomi.grad().toDouble(),
                fagsystemId
            )
        )
    }

    private fun addLinje(dag: NavHelgDag) {
        arbeisdsgiverLinjer.add(
            0,
            Utbetalingslinje(dag.dato, dag.dato, 0, 0, dag.økonomi.grad().toDouble(), fagsystemId)
        )
    }

    private interface Tilstand {
        fun betalingsdag(dag: NavDag) {}
        fun helgedag(dag: NavHelgDag) {}
        fun nyLinje(dag: NavDag) {}
        fun nyLinje(dag: NavHelgDag) {}
        fun ikkeBetalingsdag() {}
    }

    private inner class MellomLinjer : Tilstand {
        override fun betalingsdag(dag: NavDag) {
            addLinje(dag)
            tilstand = LinjeMedSats()
        }

        override fun nyLinje(dag: NavDag) {
            addLinje(dag)
            tilstand = LinjeMedSats()
        }

        override fun helgedag(dag: NavHelgDag) {
            addLinje(dag)
            tilstand = LinjeUtenSats()
        }

        override fun nyLinje(dag: NavHelgDag) {
            addLinje(dag)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeMedSats : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(dag: NavDag) {
            linje.fom = dag.dato
        }

        override fun nyLinje(dag: NavDag) {
            addLinje(dag)
        }

        override fun helgedag(dag: NavHelgDag) {
            linje.fom = dag.dato
        }

        override fun nyLinje(dag: NavHelgDag) {
            addLinje(dag)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeUtenSats : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(dag: NavDag) {
            linje.dagsats = dagStrategy(dag)
            linje.lønn = dag.økonomi.dekningsgrunnlag().toInt() //Needs to be changed for self employed
            linje.fom = dag.dato
            tilstand = LinjeMedSats()
        }

        override fun nyLinje(dag: NavDag) {
            addLinje(dag)
            tilstand = LinjeMedSats()
        }

        override fun helgedag(dag: NavHelgDag) {
            linje.fom = dag.dato
        }

        override fun nyLinje(dag: NavHelgDag) {
            addLinje(dag)
        }
    }

    private object Avsluttet : Tilstand {}
}
