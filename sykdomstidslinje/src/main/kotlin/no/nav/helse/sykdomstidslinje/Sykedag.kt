package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import java.time.LocalDateTime

internal class Sykedag internal constructor(gjelder: LocalDate, rapportert: LocalDateTime) : Dag(gjelder, rapportert, 10) {

    override fun antallSykedager() = 1

    override fun toString() = formatter.format(dagen) + "\tno.nav.helse.sykdomstidslinje.Sykedag"
}
