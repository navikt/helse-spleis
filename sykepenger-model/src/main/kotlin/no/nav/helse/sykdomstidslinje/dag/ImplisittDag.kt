package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class ImplisittDag internal constructor(
    gjelder: LocalDate,
    hendelseType: NøkkelHendelseType
) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitImplisittDag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tImplisitt dag"

    override fun nøkkel(): Nøkkel = Nøkkel.I
}
