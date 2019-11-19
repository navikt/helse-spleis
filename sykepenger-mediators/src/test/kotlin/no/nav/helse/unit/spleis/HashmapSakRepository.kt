package no.nav.helse.unit.spleis

import no.nav.helse.sak.Sak
import no.nav.helse.sak.SakObserver
import no.nav.helse.spleis.SakRepository

internal class HashmapSakRepository : SakRepository, SakObserver {

    private val map: MutableMap<String, MutableList<String>> = mutableMapOf()

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
        lagreSak(sakEndretEvent.aktørId, sakEndretEvent.memento)
    }

    private fun lagreSak(aktørId: String, memento: Sak.Memento) {
        map.computeIfAbsent(aktørId) {
            mutableListOf()
        }.add(memento.toString())
    }

    override fun hentSak(aktørId: String): Sak? {
        return map[aktørId]?.last()?.let { Sak.fromJson(it) }
    }

    fun hentHistorikk(aktørId: String): List<String> =
            map[aktørId] ?: emptyList()
}
