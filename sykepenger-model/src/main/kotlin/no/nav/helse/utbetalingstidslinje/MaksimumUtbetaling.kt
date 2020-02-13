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

    private var harRedusertUtbetaling = false

    internal fun beregn() {
        tidslinjer.forEach { it.accept(this) }
        if (harRedusertUtbetaling)
            aktivitetslogger.warnOld("Redusert utbetaling minst en dag som faller under maksimum inntekt")
        else
            aktivitetslogger.infoOld("Utbetaling har ikke blitt redusert på grunn av 6G")
    }

    override fun visitNavDag(dag: NavDag) {
        if (dag.dato in periode && dag.inntekt > `6G`.dagsats(dag.dato)) harRedusertUtbetaling = true
        dag.utbetaling = minOf(dag.inntekt, `6G`.dagsats(dag.dato)).roundToInt()
    }
}
