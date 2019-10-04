package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelse.Sykdomshendelse
import java.time.LocalDate

class Sykedag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse) : Dag(gjelder, hendelse, 10) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykedag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 1
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 1

    override fun toString() = formatter.format(dagen) + "\tSykedag"
}
