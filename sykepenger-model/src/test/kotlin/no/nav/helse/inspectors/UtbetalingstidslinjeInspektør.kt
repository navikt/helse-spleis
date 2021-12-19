package no.nav.helse.inspectors

import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import kotlin.reflect.KClass

internal val Utbetalingstidslinje.inspektør get() = UtbetalingstidslinjeInspektør(this)

// Collects assertable statistics for a Utbetalingstidslinje
internal class UtbetalingstidslinjeInspektør(private val utbetalingstidslinje: Utbetalingstidslinje): UtbetalingsdagVisitor {
    var førstedato = LocalDate.MIN
    var sistedato = LocalDate.MAX
    lateinit var førstedag: Utbetalingstidslinje.Utbetalingsdag
    lateinit var sistedag: Utbetalingstidslinje.Utbetalingsdag

    internal var arbeidsdagTeller = 0
    internal var arbeidsgiverperiodeDagTeller = 0
    internal var avvistDagTeller = 0
    internal var fridagTeller = 0
    internal var navDagTeller = 0
    internal var navHelgDagTeller = 0
    internal var foreldetDagTeller = 0
    internal var ukjentDagTeller = 0
    internal var totalUtbetaling = 0.0
    internal var totalInntekt = 0.0

    val navdager = mutableListOf<NavDag>()
    val navHelgdager = mutableListOf<NavHelgDag>()
    val arbeidsdager = mutableListOf<Arbeidsdag>()
    val arbeidsgiverdager = mutableListOf<ArbeidsgiverperiodeDag>()
    val fridager = mutableListOf<Fridag>()
    val avvistedatoer = mutableListOf<LocalDate>()
    val avvistedager = mutableListOf<AvvistDag>()

    val økonomi = mutableListOf<Økonomi>()
    val unikedager = mutableSetOf<KClass<out Utbetalingstidslinje.Utbetalingsdag>>()

    internal val size get() =
        arbeidsdagTeller +
            arbeidsgiverperiodeDagTeller +
            avvistDagTeller +
            fridagTeller +
            navDagTeller +
            navHelgDagTeller +
            ukjentDagTeller

    init {
        arbeidsdagTeller = 0
        arbeidsgiverperiodeDagTeller = 0
        avvistDagTeller = 0
        fridagTeller = 0
        navDagTeller = 0
        navHelgDagTeller = 0
        ukjentDagTeller = 0
        totalUtbetaling = 0.0
        utbetalingstidslinje.accept(this)
    }

    internal fun totalUtbetaling() = totalUtbetaling
    internal fun totalInntekt() = totalInntekt

    internal fun erNavdag(dato: LocalDate) = utbetalingstidslinje[dato] is NavDag

    private fun collect(dag: Utbetalingstidslinje.Utbetalingsdag, dato: LocalDate) {
        økonomi.add(dag.økonomi)
        unikedager.add(dag::class)
        første(dag, dato)
        sistedag = dag
        sistedato = dato
    }

    private fun første(dag: Utbetalingstidslinje.Utbetalingsdag, dato: LocalDate) {
        if (this::førstedag.isInitialized) return
        førstedag = dag
        førstedato = dato
    }

    override fun visit(
        dag: Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        arbeidsdagTeller += 1
        arbeidsdager.add(dag)
        collect(dag, dato)
    }

    override fun visit(
        dag: ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        arbeidsgiverperiodeDagTeller += 1
        arbeidsgiverdager.add(dag)
        collect(dag, dato)
    }

    override fun visit(
        dag: AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        avvistDagTeller += 1
        avvistedatoer.add(dato)
        avvistedager.add(dag)
        collect(dag, dato)
    }

    override fun visit(
        dag: Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        fridagTeller += 1
        fridager.add(dag)
        collect(dag, dato)
    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medData { _, _, _, _, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, _ ->
            totalUtbetaling += arbeidsgiverbeløp ?: 0.0
            totalUtbetaling += personbeløp ?: 0.0
            totalInntekt += aktuellDagsinntekt
        }
        navDagTeller += 1
        navdager.add(dag)
        collect(dag, dato)
    }

    override fun visit(
        dag: NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        navHelgDagTeller += 1
        navHelgdager.add(dag)
        collect(dag, dato)
    }

    override fun visit(
        dag: ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        foreldetDagTeller += 1
        collect(dag, dato)
    }

    override fun visit(
        dag: UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        ukjentDagTeller += 1
        collect(dag, dato)
    }
}
