package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class Nulldag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse): Dag(gjelder, hendelse, 0){
    override fun antallSykedager() = 0

    override fun antallSykeVirkedager(): Int = 0

    override fun tilDag() = ikkeSykedag(dagen, hendelse)

    override fun toString() = formatter.format(dagen) + "\tNulldag"
}
