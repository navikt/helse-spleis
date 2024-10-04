package no.nav.helse.inspectors

import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk

internal val Infotrygdhistorikk.inspektør get() = InfotrygdhistorikkInspektør(this)

internal class InfotrygdhistorikkInspektør(historikk: Infotrygdhistorikk) {
    private val elementer = historikk.elementer.map { Triple(it.id, it.tidsstempel, it.oppdatert) }

    fun elementer() = elementer.size
    fun opprettet(indeks: Int) = elementer.elementAt(indeks).second
    fun oppdatert(indeks: Int) = elementer.elementAt(indeks).third
}
