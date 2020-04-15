package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.UtbetalingStrategy
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
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

    override fun visitNavDag(dag: NavDag) {
        if (arbeisdsgiverLinjer.isEmpty()) return tilstand.nyLinje(dag)
        if (dag.grad == linje.grad && (linje.dagsats == 0 || linje.dagsats == dagStrategy(dag)))
            tilstand.betalingsdag(dag)
        else
            tilstand.nyLinje(dag)
    }

    override fun visitNavHelgDag(dag: NavHelgDag) {
        if (arbeisdsgiverLinjer.isEmpty() || dag.grad != linje.grad)
            tilstand.nyLinje(dag)
        else
            tilstand.helgedag(dag)
    }

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        sisteArbeidsgiverdag?.let { sisteArbeidsgiverdag = dag.dato }
        tilstand = Avsluttet
    }

    override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
        tilstand.ikkeBetalingsdag()
    }

    private fun addLinje(dag: NavDag) {
        arbeisdsgiverLinjer.add(0, Utbetalingslinje(dag.dato, dag.dato, dagStrategy(dag), dag.grad, fagsystemId))
    }

    private fun addLinje(dag: NavHelgDag) {
        arbeisdsgiverLinjer.add(0, Utbetalingslinje(dag.dato, dag.dato, 0, dag.grad, fagsystemId))
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
