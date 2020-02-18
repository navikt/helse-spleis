package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class Ubestemtdag internal constructor(dato: LocalDate, hendelseType: NøkkelHendelseType): Dag(dato, hendelseType) {
    internal constructor(left: Dag, right: Dag) : this(left.dagen, left.hendelseType)

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitUbestemt(this)
    }

    override fun toString() = formatter.format(dagen) + "\tUbestemtdag"

    override fun nøkkel(): Nøkkel = Nøkkel.Undecided
}
