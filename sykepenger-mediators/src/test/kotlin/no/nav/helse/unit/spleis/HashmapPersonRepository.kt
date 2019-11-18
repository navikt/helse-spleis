package no.nav.helse.unit.spleis

import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.spleis.PersonRepository

internal class HashmapPersonRepository : PersonRepository, PersonObserver {

    private val map: MutableMap<String, MutableList<String>> = mutableMapOf()

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
        lagrePerson(personEndretEvent.aktørId, personEndretEvent.memento)
    }

    private fun lagrePerson(aktørId: String, memento: Person.Memento) {
        map.computeIfAbsent(aktørId) {
            mutableListOf()
        }.add(memento.toString())
    }

    override fun hentPerson(aktørId: String): Person? {
        return map[aktørId]?.last()?.let { Person.fromJson(it) }
    }

    fun hentHistorikk(aktørId: String): List<String> =
            map[aktørId] ?: emptyList()
}
