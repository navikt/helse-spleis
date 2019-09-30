package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class Feriedag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse) : Dag(gjelder, hendelse, 20) {
    override fun antallSykedager() = 0
    override fun antallSykeVirkedager() = 0

    override fun toString() = formatter.format(dagen) + "\tFerie"
}
