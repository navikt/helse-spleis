package no.nav.helse.sykdomstidslinje

import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.InntektHistorie

internal class ArbeidsgiverSykdomstidslinje(
    private val sykdomstidslinjer: List<ConcreteSykdomstidslinje>,
    internal val arbeidsgiverRegler: ArbeidsgiverRegler,
    internal val inntektHistorie: InntektHistorie,
    internal val arbeidsgiverperiodeSeed: Int = 0
) : SykdomstidslinjeElement {

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.preVisitArbeidsgiver(this)
        sykdomstidslinjer.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiver(this)
    }
}
