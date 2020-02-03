package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import kotlin.math.roundToInt

internal class MaksimumUtbetaling(
    private val sykdomsgrader: Sykdomsgrader,
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogger: Aktivitetslogger
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
