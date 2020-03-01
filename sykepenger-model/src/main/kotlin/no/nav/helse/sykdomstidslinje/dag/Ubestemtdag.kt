package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class Ubestemtdag internal constructor(dato: LocalDate) : Dag(dato) {

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitUbestemt(this)
    }

    override fun toString() = formatter.format(dagen) + "\tUbestemtdag"
}
