package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

class Sykedag internal constructor(gjelder: LocalDate, hendelse: KildeHendelse) : Dag(gjelder, hendelse, 10) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykedag(this)
    }

    override fun antallSykedagerUtenHelg() = 1
    override fun antallSykedagerMedHelg() = 1

    override fun toString() = formatter.format(dagen) + "\tSykedag"
}
