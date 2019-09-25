package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

internal class Nulldag internal constructor(gjelder: LocalDate, rapportert: LocalDateTime): Dag(gjelder, rapportert, 0){

    override fun antallSykedager() = 0

    override fun tilDag() = ikkeSykedag(dagen, rapportertDato)
}
