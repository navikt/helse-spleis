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
import kotlin.math.roundToInt

internal class OppdragBuilder(
    tidslinje: Utbetalingstidslinje,
    private val orgnummer: String,
    private val fagområde: Fagområde,
    sisteDato: LocalDate = tidslinje.sisteDato(),
    private val dagStrategy: UtbetalingStrategy = NavDag.arbeidsgiverBeløp
) : UtbetalingsdagVisitor {
    private val arbeisdsgiverLinjer = mutableListOf<Utbetalingslinje>()
    private var tilstand: Tilstand = MellomLinjer()
    private val fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID())
    private var sisteArbeidsgiverdag: LocalDate? = null

    init {
        tidslinje.kutt(sisteDato).reverse().accept(this)
    }

    internal fun result(): Oppdrag {
        arbeisdsgiverLinjer.removeAll { it.beløp == 0 }
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
        økonomi.mediatorDetails { grad, _, _, aktuellDagsinntekt, _, _, _ ->
            if (arbeisdsgiverLinjer.isEmpty()) return@mediatorDetails tilstand.nyLinje(dag, dato, grad, aktuellDagsinntekt)
            if (grad == linje.grad && (linje.beløp == 0 || linje.beløp == dagStrategy(dag)))
                tilstand.betalingsdag(dag, dato, grad, aktuellDagsinntekt)
            else
                tilstand.nyLinje(dag, dato, grad, aktuellDagsinntekt)
        }
    }

    override fun visit(
        dag: NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.mediatorDetails { grad, _, _, aktuellDagsinntekt, _, _, _ ->
            if (arbeisdsgiverLinjer.isEmpty() || grad != linje.grad)
                tilstand.nyLinje(dag, dato, grad)
            else
                tilstand.helgedag(dag, dato, grad)
        }
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

    private fun addLinje(dag: NavDag, dato: LocalDate, grad: Double, aktuellDagsinntekt: Double) {
        arbeisdsgiverLinjer.add(
            0,
            Utbetalingslinje(
                dato,
                dato,
                dagStrategy(dag),
                aktuellDagsinntekt.roundToInt(),
                grad,
                fagsystemId
            )
        )
    }

    private fun addLinje(dag: NavHelgDag, dato: LocalDate, grad: Double) {
        arbeisdsgiverLinjer.add(
            0,
            Utbetalingslinje(dato, dato, 0, 0, grad, fagsystemId)
        )
    }

    internal interface Tilstand {
        fun betalingsdag(
            dag: NavDag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {}
        fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {}
        fun nyLinje(
            dag: NavDag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {}
        fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {}
        fun ikkeBetalingsdag() {}
    }

    private inner class MellomLinjer : Tilstand {
        override fun betalingsdag(
            dag: NavDag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
            tilstand = LinjeMedSats()
        }

        override fun nyLinje(
            dag: NavDag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
            tilstand = LinjeMedSats()
        }

        override fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
            addLinje(dag, dato, grad)
            tilstand = LinjeUtenSats()
        }

        override fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
            addLinje(dag, dato, grad)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeMedSats : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(
            dag: NavDag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            linje.fom = dag.dato
        }

        override fun nyLinje(
            dag: NavDag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
        }

        override fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
            linje.fom = dag.dato
        }

        override fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
            addLinje(dag, dato, grad)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeUtenSats : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(
            dag: NavDag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            linje.beløp = dagStrategy(dag)
            linje.aktuellDagsinntekt = aktuellDagsinntekt.roundToInt() //Needs to be changed for self employed
            linje.fom = dag.dato
            tilstand = LinjeMedSats()
        }

        override fun nyLinje(
            dag: NavDag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
            tilstand = LinjeMedSats()
        }

        override fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
            linje.fom = dag.dato
        }

        override fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
            addLinje(dag, dato, grad)
        }
    }

    private object Avsluttet : Tilstand {}
}
