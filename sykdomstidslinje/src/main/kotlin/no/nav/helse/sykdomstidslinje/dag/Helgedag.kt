package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.DayOfWeek
import java.time.LocalDate

class Helgedag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse): Dag(gjelder, hendelse){
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitHelgedag(this)
    }

    init {
        require(gjelder.dayOfWeek == DayOfWeek.SATURDAY || gjelder.dayOfWeek == DayOfWeek.SUNDAY) {"En ikkeSykedag må være lørdag eller søndag"}
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tHelg"

    override fun dagType(): JsonDagType = JsonDagType.HELGEDAG

    override fun nøkkel(): Nøkkel = Nøkkel.W
}
