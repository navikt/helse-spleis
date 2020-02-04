package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class Studiedag internal constructor(gjelder: LocalDate, hendelseType: NøkkelHendelseType): Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitStudiedag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tStudiedag"

    override fun dagType() = JsonDagType.STUDIEDAG

    override fun nøkkel(): Nøkkel = Nøkkel.EDU
}
