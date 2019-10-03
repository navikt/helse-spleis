package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

class Feriedag internal constructor(gjelder: LocalDate, hendelse: KildeHendelse) : Dag(gjelder, hendelse, 20) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitFeriedag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 0
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tFerie"
}
