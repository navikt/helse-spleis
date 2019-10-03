package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

class SykHelgedag internal constructor(gjelder: LocalDate, hendelse: KildeHendelse): Dag(gjelder, hendelse, 10) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykHelgedag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 1
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tSykedag helg"
}
