package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

abstract class Dag internal constructor(private val dagen: LocalDate, private val rapportertDato: LocalDateTime) :
    Interval() {
    override fun startdato() = dagen
    override fun sluttdato() = dagen

    override fun flatten(): List<Dag> {
        return listOf(this)
    }

    override fun rapportertDato() = rapportertDato
}
