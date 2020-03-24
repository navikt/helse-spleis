package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.NySykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class Studiedag internal constructor(gjelder: LocalDate) : Dag(gjelder) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitStudiedag(this)
    }

    override fun accept(visitor: NySykdomstidslinjeVisitor) {
        visitor.visitStudiedag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tStudiedag"
}
