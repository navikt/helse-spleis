package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import java.time.format.DateTimeFormatter

abstract class Dag internal constructor(internal val dagen: LocalDate, internal val hendelse: KildeHendelse, private val prioritet: Int) :
    Sykdomstidslinje(), Comparable<Dag>{
    internal val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val erstatter: MutableList<Dag> = mutableListOf()

    override fun startdato() = dagen
    override fun sluttdato() = dagen
    override fun flatten() = listOf(this)
    override fun dag(dato: LocalDate, hendelse: KildeHendelse) = if(dato == dagen) this else Nulldag(dagen, hendelse)

    internal fun erstatter(dager: List<Dag>) {
        erstatter.addAll(dager.filterNot {
            it is Nulldag
        })
    }
    fun dagerErstattet(): List<Dag> = erstatter

    override fun compareTo(other: Dag): Int {
        val resultat = this.prioritet.compareTo(other.prioritet)
        return if (resultat == 0) this.hendelse.rapportertdato().compareTo(other.hendelse.rapportertdato()) else resultat
    }

    internal open fun tilDag() = this

    override fun length() = 1

    override fun sisteHendelse() = this.hendelse
}

