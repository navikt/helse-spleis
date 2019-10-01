package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class Nulldag internal constructor(gjelder: LocalDate, hendelse: KildeHendelse): Dag(gjelder, hendelse, 0){
    override fun accept(visitor: SykdomstidslinjeVisitor) {}

    override fun antallSykedagerUtenHelg() = 0

    override fun antallSykedagerMedHelg(): Int = 0

    override fun tilDag() = ikkeSykedag(dagen, hendelse)

    override fun toString() = formatter.format(dagen) + "\tNulldag"
}
