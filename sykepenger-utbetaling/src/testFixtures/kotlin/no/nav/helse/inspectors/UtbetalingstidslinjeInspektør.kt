package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodedagNav
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ForeldetDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import kotlin.reflect.KClass

val Utbetalingstidslinje.inspektør get() = UtbetalingstidslinjeInspektør(this)

// Collects assertable statistics for an Utbetalingstidslinje
class UtbetalingstidslinjeInspektør(private val utbetalingstidslinje: Utbetalingstidslinje) {
    val førstedato = utbetalingstidslinje.firstOrNull()?.dato ?: LocalDate.MIN
    val sistedato = utbetalingstidslinje.lastOrNull()?.dato ?: LocalDate.MAX

    var arbeidsdagTeller = 0
    var arbeidsgiverperiodeDagTeller = 0
    var arbeidsgiverperiodedagNavTeller = 0
    var avvistDagTeller = 0
    var fridagTeller = 0
    var navDagTeller = 0
    var navHelgDagTeller = 0
    var foreldetDagTeller = 0
    var ukjentDagTeller = 0
    var totalUtbetaling = 0.0

    val navdager = mutableListOf<NavDag>()
    val navHelgdager = mutableListOf<NavHelgDag>()
    val arbeidsdager = mutableListOf<Arbeidsdag>()
    val arbeidsgiverdager = mutableListOf<ArbeidsgiverperiodeDag>()
    val arbeidsgiverperiodedagerNavAnsvar = mutableListOf<ArbeidsgiverperiodedagNav>()
    val fridager = mutableListOf<Fridag>()
    val avvistedatoer = mutableListOf<LocalDate>()
    val avvistedager = mutableListOf<AvvistDag>()
    private val begrunnelser = mutableMapOf<LocalDate, List<Begrunnelse>>()

    private val økonomi = mutableMapOf<LocalDate, Økonomi>()
    val unikedager = mutableSetOf<KClass<out Utbetalingsdag>>()

    val size get() =
        arbeidsdagTeller +
            arbeidsgiverperiodeDagTeller +
            avvistDagTeller +
            fridagTeller +
            navDagTeller +
            navHelgDagTeller +
            foreldetDagTeller +
            ukjentDagTeller +
                arbeidsgiverperiodedagNavTeller

    init {
        arbeidsdagTeller = 0
        arbeidsgiverperiodeDagTeller = 0
        avvistDagTeller = 0
        fridagTeller = 0
        navDagTeller = 0
        navHelgDagTeller = 0
        ukjentDagTeller = 0
        totalUtbetaling = 0.0

        utbetalingstidslinje.forEach { dag ->
            when (dag) {
                is Arbeidsdag -> {
                    arbeidsdagTeller += 1
                    arbeidsdager.add(dag)
                    collect(dag, dag.dato, dag.økonomi)
                }
                is ArbeidsgiverperiodeDag -> {
                    arbeidsgiverperiodeDagTeller += 1
                    arbeidsgiverdager.add(dag)
                    collect(dag, dag.dato, dag.økonomi)
                }
                is ArbeidsgiverperiodedagNav -> {
                    arbeidsgiverperiodedagNavTeller += 1
                    arbeidsgiverperiodedagerNavAnsvar.add(dag)
                    collect(dag, dag.dato, dag.økonomi)
                }
                is AvvistDag -> {
                    avvistDagTeller += 1
                    avvistedatoer.add(dag.dato)
                    avvistedager.add(dag)
                    begrunnelser[dag.dato] = dag.begrunnelser
                    collect(dag, dag.dato, dag.økonomi)
                }
                is ForeldetDag -> {
                    foreldetDagTeller += 1
                    collect(dag, dag.dato, dag.økonomi)
                }
                is Fridag -> {
                    fridagTeller += 1
                    fridager.add(dag)
                    collect(dag, dag.dato, dag.økonomi)
                }
                is NavDag -> {
                    totalUtbetaling += dag.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0
                    totalUtbetaling += dag.økonomi.personbeløp?.dagligInt ?: 0
                    navDagTeller += 1
                    navdager.add(dag)
                    collect(dag, dag.dato, dag.økonomi)
                }
                is NavHelgDag -> {
                    navHelgDagTeller += 1
                    navHelgdager.add(dag)
                    collect(dag, dag.dato, dag.økonomi)
                }
                is UkjentDag -> {
                    ukjentDagTeller += 1
                    collect(dag, dag.dato, dag.økonomi)
                }
            }
        }
    }

    fun grad(dag: LocalDate) = økonomi.getValue(dag).brukAvrundetGrad { grad -> grad }
    fun arbeidsgiverbeløp(dag: LocalDate) = økonomi.getValue(dag).inspektør.arbeidsgiverbeløp

    fun totalUtbetaling() = totalUtbetaling

    fun begrunnelse(dato: LocalDate) =
        begrunnelser[dato] ?: emptyList()

    fun erNavdag(dato: LocalDate) = utbetalingstidslinje[dato] is NavDag

    private fun collect(dag: Utbetalingsdag, dato: LocalDate, økonomi: Økonomi) {
        this.økonomi[dato] = økonomi
        unikedager.add(dag::class)
    }
}
