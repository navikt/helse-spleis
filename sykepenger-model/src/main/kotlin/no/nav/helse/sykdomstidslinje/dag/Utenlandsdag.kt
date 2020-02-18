package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class Utenlandsdag internal constructor(gjelder: LocalDate, hendelseType: Kildehendelse) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitUtenlandsdag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tUtenlandsdag"

    override fun turneringsnøkkel(): Turneringsnøkkel = Turneringsnøkkel.DA
}
