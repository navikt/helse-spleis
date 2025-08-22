package no.nav.helse.etterlevelse

import java.time.LocalDate
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi

internal class UtbetalingstidslinjeBuilder(utbetalingstidslinje: Utbetalingstidslinje) {
    private var forrigeDag: Tidslinjedag? = null
    private val navdager = utbetalingstidslinje.mapNotNull { dag ->
        when (dag) {
            is Utbetalingsdag.Arbeidsdag -> null
            is Utbetalingsdag.ArbeidsgiverperiodeDag -> visit(dag.dato, "AGPDAG", dag.økonomi)
            is Utbetalingsdag.Ventetidsdag -> visit(dag.dato, "VENTETIDSDAG", dag.økonomi)
            is Utbetalingsdag.ArbeidsgiverperiodedagNav -> null
            is Utbetalingsdag.AvvistDag -> visit(dag.dato, "AVVISTDAG", dag.økonomi)
            is Utbetalingsdag.ForeldetDag -> null
            is Utbetalingsdag.Fridag -> {
                // Dersom vi er inne i en oppholdsperiode ønsker vi ikke å ta med vanlige helger
                if (forrigeDag?.erRettFør(dag.dato) == true)
                    visit(dag.dato, "FRIDAG", dag.økonomi)
                else null
            }

            is Utbetalingsdag.NavDag -> visit(dag.dato, "NAVDAG", dag.økonomi)
            is Utbetalingsdag.NavHelgDag -> {
                if (forrigeDag?.erAvvistDag() == true)
                    visit(dag.dato, "AVVISTDAG", dag.økonomi)
                else
                    visit(dag.dato, "NAVDAG", dag.økonomi)
            }

            is Utbetalingsdag.UkjentDag -> null
        }.also { forrigeDag = it }
    }

    fun dager() = navdager.toList()

    private fun visit(dato: LocalDate, dagtype: String, økonomi: Økonomi?): Tidslinjedag {
        val grad = økonomi?.brukAvrundetGrad { grad -> grad }
        return Tidslinjedag(dato, dagtype, grad)
    }

    companion object {
        internal fun Utbetalingstidslinje.subsumsjonsformat(): List<Tidslinjedag> = UtbetalingstidslinjeBuilder(this).dager()
        internal fun List<Utbetalingstidslinje>.subsumsjonsformat(): List<List<Tidslinjedag>> = map { it.subsumsjonsformat() }
            .filter { it.isNotEmpty() }
    }
}
