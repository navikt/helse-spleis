package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.*
import no.nav.helse.utbetalingslinjer.Utbetalingslinje.Companion.kjedeSammenLinjer

internal class OppdragBuilder(
    private val tidslinje: Utbetalingstidslinje,
    private val mottaker: String,
    private val fagområde: Fagområde,
    sisteDato: LocalDate = tidslinje.periode().endInclusive,
    fagsystemId: String? = null
) : UtbetalingsdagVisitor {
    private val fagsystemId = fagsystemId ?: genererUtbetalingsreferanse(UUID.randomUUID())
    private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()
    private var tilstand: Tilstand = MellomLinjer()
    private var sisteArbeidsgiverdag: LocalDate? = null

    init {
        tidslinje.kutt(sisteDato).reverse().accept(this)
    }

    internal fun build(tidligere: Oppdrag?, aktivitetslogg: IAktivitetslogg): Oppdrag {
        val oppdrag = nyttOppdrag()
        return when (tidligere) {
            null -> oppdrag
            else -> oppdrag.minus(tidligere, aktivitetslogg)
        }.also {
            aktivitetslogg.info(
                if (it.isEmpty()) "Ingen utbetalingslinjer bygget"
                else "Utbetalingslinjer bygget vellykket"
            )
        }
    }

    private fun nyttOppdrag(): Oppdrag {
        val eldsteDag = utbetalingslinjer.firstOrNull()?.fom?.minusDays(1)
        val linjerMedBeløp = fjernLinjerUtenUtbetalingsdager(utbetalingslinjer)
        val kjededeLinjer = kjedeSammenLinjer(linjerMedBeløp)
        val ferdigLinjer = førsteLinjeSkalIkkePekePåAndreLinjer(kjededeLinjer)
        return Oppdrag(mottaker, fagområde, ferdigLinjer, fagsystemId, sisteArbeidsgiverdag ?: eldsteDag)
    }

    private fun fjernLinjerUtenUtbetalingsdager(linjer: List<Utbetalingslinje>) =
        linjer.filterNot { it.beløp == null || it.beløp == 0 }
    private fun førsteLinjeSkalIkkePekePåAndreLinjer(linjer: List<Utbetalingslinje>) =
        listOfNotNull(linjer.firstOrNull()?.førsteLinje()) + linjer.drop(1)

    private val linje get() = utbetalingslinjer.first()

    override fun visit(dag: UkjentDag, dato: LocalDate, økonomi: Økonomi) {
        tilstand.sisteArbeidsgiverdag(dag.dato)
        tilstand = Avsluttet
    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        // TODO: OppdragBuilder må bruke grad som Int, altså avrundetData
        økonomi.medAvrundetData { grad, _, _, _, _, aktuellDagsinntekt, _, _, _ ->
            if (utbetalingslinjer.isNotEmpty() && fagområde.kanLinjeUtvides(linje, dag.beløpkilde(), grad))
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
        // TODO: OppdragBuilder må bruke grad som Int, altså avrundetData
        økonomi.medAvrundetData { grad, _ ->
           if (utbetalingslinjer.isNotEmpty() && grad != linje.grad)
                tilstand.nyLinje(dag, dato, grad)
            else
                tilstand.helgedag(dag, dato, grad)
        }
    }

    override fun visit(
        dag: Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visit(
        dag: ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.sisteArbeidsgiverdag(dag.dato)
        tilstand = Avsluttet
    }

    override fun visit(
        dag: AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visit(
        dag: Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.ikkeBetalingsdag()
    }

    override fun visit(
        dag: ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        tilstand.ikkeBetalingsdag()
    }

    private fun addLinje(dag: Utbetalingsdag, dato: LocalDate, grad: Int, aktuellDagsinntekt: Int) {
        utbetalingslinjer.add(0, fagområde.linje(fagsystemId, dag.beløpkilde(), dato, grad, aktuellDagsinntekt))
    }

    private fun addLinje(dato: LocalDate, grad: Int) {
        utbetalingslinjer.add(0, fagområde.linje(fagsystemId, dato, grad))
    }

    internal interface Tilstand {
        fun sisteArbeidsgiverdag(dato: LocalDate) {}

        fun betalingsdag(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Int,
            aktuellDagsinntekt: Int
        ) {
        }

        fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Int
        ) {
        }

        fun nyLinje(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Int,
            aktuellDagsinntekt: Int
        ) {
        }

        fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Int
        ) {
        }

        fun ikkeBetalingsdag() {}
    }

    private inner class MellomLinjer : Tilstand {
        override fun sisteArbeidsgiverdag(dato: LocalDate) {
            sisteArbeidsgiverdag = dato
        }

        override fun betalingsdag(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Int,
            aktuellDagsinntekt: Int
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
            tilstand = LinjeMedSats()
        }


        override fun nyLinje(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Int,
            aktuellDagsinntekt: Int
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
            tilstand = LinjeMedSats()
        }


        override fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Int
        ) {
            /* ønsker ikke slutte en linje på helg */
        }

        override fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(dato, grad)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeMedSats : Tilstand {
        override fun sisteArbeidsgiverdag(dato: LocalDate) {
            sisteArbeidsgiverdag = dato
        }

        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Int,
            aktuellDagsinntekt: Int
        ) {
            linje.fom = dag.dato
        }

        override fun nyLinje(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Int,
            aktuellDagsinntekt: Int
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
        }

        override fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Int
        ) {
            linje.fom = dag.dato
        }

        override fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(dato, grad)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeUtenSats : Tilstand {
        override fun sisteArbeidsgiverdag(dato: LocalDate) {
            sisteArbeidsgiverdag = dato
        }

        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Int,
            aktuellDagsinntekt: Int
        ) {
            val førsteLinje = utbetalingslinjer.removeFirst()
            utbetalingslinjer.add(0, fagområde.oppdaterLinje(førsteLinje, dag.dato, dag.beløpkilde(), aktuellDagsinntekt))
            tilstand = LinjeMedSats()
        }

        override fun nyLinje(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Int,
            aktuellDagsinntekt: Int
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
            tilstand = LinjeMedSats()
        }

        override fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Int
        ) {
            linje.fom = dag.dato
        }

        override fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Int
        ) {
            addLinje(dato, grad)
        }
    }

    private object Avsluttet : Tilstand {}
}
