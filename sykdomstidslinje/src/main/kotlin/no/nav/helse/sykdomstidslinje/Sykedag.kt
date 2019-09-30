package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class Sykedag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse) : Dag(gjelder, hendelse, 10) {

    override fun antallSykedager() = 1
    override fun antallSykeVirkedager() = 1

    override fun toString() = formatter.format(dagen) + "\tSykedag"
}
