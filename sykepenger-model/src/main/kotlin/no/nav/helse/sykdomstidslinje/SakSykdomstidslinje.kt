package no.nav.helse.sykdomstidslinje

import no.nav.helse.utbetalingstidslinje.AlderRegler
import no.nav.helse.utbetalingstidslinje.UtbetalingBuilder
import java.time.LocalDate

internal class SakSykdomstidslinje(
    private val sykdomstidslinjer: List<ArbeidsgiverSykdomstidslinje>,
    private val alderRegler: AlderRegler,
    private val arbeidsgiverSykdomstidslinje: ArbeidsgiverSykdomstidslinje,
    private val førsteDag: LocalDate,
    private val sisteDag: LocalDate

) : SykdomstidslinjeElement {

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.preVisitSak(this)
        sykdomstidslinjer.forEach { it.accept(visitor) }
        visitor.postVisitSak(this)
    }

    internal fun utbetalingslinjer(): List<Utbetalingslinje> {
        val tidslinje = UtbetalingBuilder(arbeidsgiverSykdomstidslinje,sisteDag).result()
        val tidslinjer = (sykdomstidslinjer - arbeidsgiverSykdomstidslinje).map { UtbetalingBuilder(it,sisteDag).result() }
        return tidslinje.utbetalingslinjer(tidslinjer, alderRegler, førsteDag, sisteDag)
    }
}
