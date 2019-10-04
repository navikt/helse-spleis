package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelse.Sykdomshendelse
import java.time.LocalDate

internal class Nulldag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse): Dag(gjelder, hendelse, 0){
    override fun accept(visitor: SykdomstidslinjeVisitor) {}

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun antallSykedagerHvorViIkkeTellerMedHelg(): Int = 0

    override fun tilDag() = ikkeSykedag(dagen, hendelse)

    override fun toString() = formatter.format(dagen) + "\tNulldag"
}
