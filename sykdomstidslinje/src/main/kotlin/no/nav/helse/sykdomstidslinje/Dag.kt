package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import java.time.format.DateTimeFormatter

abstract class Dag internal constructor(internal val dagen: LocalDate, internal val hendelse: Sykdomshendelse, private val prioritet: Int) :
    Sykdomstidslinje(), Comparable<Dag>{
    internal val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    override fun startdato() = dagen
    override fun sluttdato() = dagen
    override fun flatten() = listOf(this)
    override fun dag(dato: LocalDate, hendelse: Sykdomshendelse) = if(dato == dagen) this else Nulldag(dagen, hendelse)

    override fun compareTo(other: Dag): Int {
        val resultat = this.prioritet.compareTo(other.prioritet)
        return if (resultat == 0) this.hendelse.rapportertdato().compareTo(other.hendelse.rapportertdato()) else resultat
    }

    open fun tilDag() = this

    override fun length() = 1

    override fun sisteHendelse() = this.hendelse
}

