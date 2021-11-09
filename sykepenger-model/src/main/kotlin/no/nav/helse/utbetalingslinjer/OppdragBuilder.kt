package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.*
import kotlin.math.roundToInt

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

    internal fun build(tidligere: Oppdrag?, aktivitetslogg: IAktivitetslogg) = when (tidligere) {
        null -> nyttOppdrag()
        else -> oppdragBasertPåTidligere(tidligere, aktivitetslogg)
    }.also {
        aktivitetslogg.info(
            if (it.isEmpty()) "Ingen utbetalingslinjer bygget"
            else "Utbetalingslinjer bygget vellykket"
        )
    }

    private fun nyttOppdrag(): Oppdrag {
        fjernLinjerUtenUtbetalingsdager()
        kjedeSammenLinjer()
        return Oppdrag(mottaker, fagområde, utbetalingslinjer, fagsystemId, sisteArbeidsgiverdag)
    }

    private fun fjernLinjerUtenUtbetalingsdager() {
        utbetalingslinjer.removeAll { it.beløp == null || it.beløp == 0 }
    }
    private fun kjedeSammenLinjer() {
        utbetalingslinjer.zipWithNext { a, b -> b.kobleTil(a) }
        førsteLinjeSkalIkkePekePåAndreLinjer()
    }
    private fun førsteLinjeSkalIkkePekePåAndreLinjer() {
        utbetalingslinjer.firstOrNull()?.refFagsystemId = null
    }

    private fun oppdragBasertPåTidligere(tidligere: Oppdrag, aktivitetslogg: IAktivitetslogg) =
        nyttOppdrag()
            .minus(tidligere, aktivitetslogg)
            .also {
                if (tidligere.fagsystemId() == it.fagsystemId()) it.nettoBeløp(tidligere)
            }

    private val linje get() = utbetalingslinjer.first()

    override fun visit(dag: UkjentDag, dato: LocalDate, økonomi: Økonomi) {
        tilstand = Avsluttet
    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medData { grad, aktuellDagsinntekt ->
            if (utbetalingslinjer.isEmpty()) return@medData tilstand.nyLinje(dag, dato, grad, aktuellDagsinntekt!!)
            if (grad == linje.grad && (linje.beløp == null || linje.beløp == fagområde.beløp(dag.økonomi)))
                tilstand.betalingsdag(dag, dato, grad, aktuellDagsinntekt!!)
            else
                tilstand.nyLinje(dag, dato, grad, aktuellDagsinntekt!!)
        }
    }

    override fun visit(
        dag: NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medData { grad, _ ->
            if (utbetalingslinjer.isEmpty() || grad != linje.grad)
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
        if (sisteArbeidsgiverdag == null) sisteArbeidsgiverdag = dag.dato
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

    private fun addLinje(dag: Utbetalingsdag, dato: LocalDate, grad: Double, aktuellDagsinntekt: Double) {
        utbetalingslinjer.add(
            0,
            Utbetalingslinje(
                dato,
                dato,
                Satstype.DAG,
                fagområde.beløp(dag.økonomi),
                aktuellDagsinntekt.roundToInt(),
                grad,
                fagsystemId,
                klassekode = fagområde.klassekode()
            )
        )
    }


    private fun addLinje(dato: LocalDate, grad: Double) {
        utbetalingslinjer.add(
            0,
            Utbetalingslinje(dato, dato, Satstype.DAG, null, 0, grad, fagsystemId)
        )
    }


    internal interface Tilstand {
        fun betalingsdag(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
        }

        fun helgedag(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
        }

        fun nyLinje(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
        }

        fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
        }

        fun ikkeBetalingsdag() {}
    }

    private inner class MellomLinjer : Tilstand {
        override fun betalingsdag(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            addLinje(dag, dato, grad, aktuellDagsinntekt)
            tilstand = LinjeMedSats()
        }


        override fun nyLinje(
            dag: Utbetalingsdag,
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
            addLinje(dato, grad)
            tilstand = LinjeUtenSats()
        }

        override fun nyLinje(
            dag: NavHelgDag,
            dato: LocalDate,
            grad: Double
        ) {
            addLinje(dato, grad)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeMedSats : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            linje.fom = dag.dato
        }

        override fun nyLinje(
            dag: Utbetalingsdag,
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
            addLinje(dato, grad)
            tilstand = LinjeUtenSats()
        }
    }

    private inner class LinjeUtenSats : Tilstand {
        override fun ikkeBetalingsdag() {
            tilstand = MellomLinjer()
        }

        override fun betalingsdag(
            dag: Utbetalingsdag,
            dato: LocalDate,
            grad: Double,
            aktuellDagsinntekt: Double
        ) {
            linje.beløp = fagområde.beløp(dag.økonomi)
            linje.aktuellDagsinntekt = aktuellDagsinntekt.roundToInt()
            linje.fom = dag.dato
            tilstand = LinjeMedSats()
        }

        override fun nyLinje(
            dag: Utbetalingsdag,
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
            addLinje(dato, grad)
        }
    }

    private object Avsluttet : Tilstand {}
}
