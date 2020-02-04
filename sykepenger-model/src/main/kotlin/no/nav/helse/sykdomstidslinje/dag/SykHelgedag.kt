package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class SykHelgedag internal constructor(gjelder: LocalDate, hendelseType: NøkkelHendelseType): Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykHelgedag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tSykedag helg"

    override fun dagType() = JsonDagType.SYK_HELGEDAG

    override fun nøkkel(): Nøkkel = Nøkkel.SW
}
