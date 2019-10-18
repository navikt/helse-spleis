package no.nav.helse.unit.person

import no.nav.helse.person.PersonRepository
import no.nav.helse.person.domain.Person

class HashmapPersonRepository : PersonRepository {
    private val map: MutableMap<String, MutableList<String>> = mutableMapOf()

    override fun lagrePerson(person: Person) {
        map.computeIfAbsent(person.aktørId) {
            mutableListOf()
        }.add(person.toJson())
    }

    override fun hentPerson(aktørId: String): Person? {
        return map[aktørId]?.last()?.let { Person.fromJson(it) }
    }

    fun hentHistorikk(aktørId: String): List<String> =
        map[aktørId] ?: emptyList()
}