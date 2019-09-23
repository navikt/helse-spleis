package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

class Arbeidsdag(private val arbeidsdagenGjelder: LocalDate, private val tidspunktRapportert: LocalDateTime): Interval {
    override fun startdato() = arbeidsdagenGjelder
    override fun sluttdato() = arbeidsdagenGjelder
    override fun antallSykedager() = 0
}
