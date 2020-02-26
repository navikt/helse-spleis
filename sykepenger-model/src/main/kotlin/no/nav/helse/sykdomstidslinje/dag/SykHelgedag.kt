package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class SykHelgedag internal constructor(gjelder: LocalDate, override val grad: Double) : DagMedGrad, Dag(gjelder) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykHelgedag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tSykedag helg"
}
