package no.nav.helse.sykdomstidslinje

import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.UtbetalingBuilder
import java.time.LocalDate

internal class PersonSykdomstidslinje(
    private val sykdomstidslinjer: List<ArbeidsgiverSykdomstidslinje>,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val arbeidsgiverSykdomstidslinje: ArbeidsgiverSykdomstidslinje,
    private val førsteDag: LocalDate,
    private val sisteDag: LocalDate

) : SykdomstidslinjeElement {

    private var maksdato: LocalDate? = null

    internal fun maksdato() = maksdato

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.preVisitSak(this)
        sykdomstidslinjer.forEach { it.accept(visitor) }
        visitor.postVisitSak(this)
    }

    internal fun utbetalingslinjer(sisteNavDagForArbeidsgiverFørPerioden: LocalDate?): List<Utbetalingslinje> {
        val tidslinje = UtbetalingBuilder(arbeidsgiverSykdomstidslinje,sisteDag, sisteNavDagForArbeidsgiverFørPerioden).result()
        val tidslinjer = (sykdomstidslinjer - arbeidsgiverSykdomstidslinje).map { UtbetalingBuilder(it,sisteDag, null).result() }
        return tidslinje.utbetalingslinjer(tidslinjer, alder, arbeidsgiverRegler, førsteDag, sisteDag)
            .also {
                maksdato = tidslinje.maksdato()
            }
    }
}
