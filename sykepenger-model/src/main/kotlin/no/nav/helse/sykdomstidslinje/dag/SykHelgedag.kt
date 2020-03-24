package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.NySykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class SykHelgedag(gjelder: LocalDate, grad: Double) : GradertDag(gjelder, grad) {

    override fun toString() = formatter.format(dagen) + "\tSykedag helg ($grad %)"

    internal class Søknad(gjelder: LocalDate, grad: Double) : SykHelgedag(gjelder, grad) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitSykHelgedag(this)
        }

        override fun accept(visitor: NySykdomstidslinjeVisitor) {
            visitor.visitSykHelgedag(this)
        }
    }

    internal class Sykmelding(gjelder: LocalDate, grad: Double) : SykHelgedag(gjelder, grad) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitSykHelgedag(this)
        }

        override fun accept(visitor: NySykdomstidslinjeVisitor) {
            visitor.visitSykHelgedag(this)
        }
    }
}
