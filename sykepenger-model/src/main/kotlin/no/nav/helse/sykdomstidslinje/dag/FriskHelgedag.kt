package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.person.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class FriskHelgedag(gjelder: LocalDate) : Dag(gjelder) {

    override fun toString() = formatter.format(dagen) + "\tFrisk helgedag"

    internal class Inntektsmelding(gjelder: LocalDate) : FriskHelgedag(gjelder) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitFriskHelgedag(this)
        }
    }

    internal class Søknad(gjelder: LocalDate) : FriskHelgedag(gjelder) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitFriskHelgedag(this)
        }
    }
}
