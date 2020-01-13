package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import kotlin.math.roundToInt

internal class MaksimumUtbetaling(
    private val sykdomsgrader: Sykdomsgrader,
    private val tidslinjer: List<Utbetalingstidslinje>
): Utbetalingstidslinje.UtbetalingsdagVisitor {

    init {
        require(tidslinjer.size == 1) {"Flere arbeidsgivere er ikke støttet enda"}
    }

    internal fun beregn() {
        tidslinjer.forEach { it.accept(this) }
    }

    override fun visitNavDag(dag: NavDag) {
        dag.utbetaling = minOf(dag.inntekt, `6G`.dagsats(dag.dato)).roundToInt()
    }
}
