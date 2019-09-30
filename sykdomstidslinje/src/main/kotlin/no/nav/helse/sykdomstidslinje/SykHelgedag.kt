package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

class SykHelgedag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse): Dag(gjelder, hendelse, 10) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykHelgedag(this)
    }

    override fun antallSykedagerUtenHelg() = 1
    override fun antallSykedagerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tSykedag helg"
}
