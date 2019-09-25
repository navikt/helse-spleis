package no.nav.helse.util.interval

import java.time.LocalDate
import java.time.LocalDateTime

abstract class Dag internal constructor(private val dagen: LocalDate, private val rapportertDato: LocalDateTime, private val prioritet: Int) :
    Interval(), Comparable<Dag>{
    override fun startdato() = dagen
    override fun sluttdato() = dagen

    override fun flatten() = listOf(this)

    override fun rapportertDato() = rapportertDato

    override fun dag(dato: LocalDate) = if(dato == dagen) this else Nulldag(dagen, rapportertDato)

    override fun compareTo(other: Dag): Int {
        val resultat = this.prioritet.compareTo(other.prioritet)
        return if (resultat == 0) this.rapportertDato.compareTo(other.rapportertDato) else resultat
    }
}

