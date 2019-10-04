package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelse.Sykdomshendelse
import java.time.DayOfWeek
import java.time.LocalDate

class Helgedag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse): Dag(gjelder, hendelse, 20){
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitHelgedag(this)
    }

    init {
        require(gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY) {"En ikkeSykedag må være lørdag eller søndag"}
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tHelg"
}
