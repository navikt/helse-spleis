package no.nav.helse.sykdomstidslinje

import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Inntektsberegner
import java.time.LocalDate

@Deprecated("EPIC 1")
internal class ArbeidsgiverSykdomstidslinje(
    private val sykdomstidslinjer: List<ConcreteSykdomstidslinje>,
    internal val arbeidsgiverRegler: ArbeidsgiverRegler,
    internal val inntektsberegner: Inntektsberegner
) : SykdomstidslinjeElement {

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.preVisitArbeidsgiver(this)
        sykdomstidslinjer.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiver(this)
    }

    internal fun kutt(sisteDag: LocalDate): ArbeidsgiverSykdomstidslinje {

        return ArbeidsgiverSykdomstidslinje(
            sykdomstidslinjer.mapNotNull { it.kutt(sisteDag) },
            arbeidsgiverRegler,
            inntektsberegner
        )
    }
}
