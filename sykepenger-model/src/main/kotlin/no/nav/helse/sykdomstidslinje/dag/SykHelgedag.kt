package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class SykHelgedag(gjelder: LocalDate, override val grad: Double) : DagMedGrad, Dag(gjelder) {

    override fun toString() = formatter.format(dagen) + "\tSykedag helg"

    internal class Søknad(gjelder: LocalDate, grad: Double) : SykHelgedag(gjelder, grad) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitSykHelgedag(this)
        }
    }

    internal class Sykmelding(gjelder: LocalDate, grad: Double) : SykHelgedag(gjelder, grad) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitSykHelgedag(this)
        }
    }
}
