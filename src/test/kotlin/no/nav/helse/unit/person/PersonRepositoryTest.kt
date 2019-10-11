package no.nav.helse.unit.person

import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.person.PersonMediator
import no.nav.helse.person.PersonRepository
import no.nav.helse.person.domain.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class PersonRepositoryTest {

    private class HashmapRepository : PersonRepository {
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

    @Test
    internal fun `mediator lagrer person ved endringer`() {
        val repo = HashmapRepository()
        val mediator = PersonMediator(
                personRepository = repo
        )

        mediator.håndterNySøknad(nySøknad(
                aktørId = "1234"
        ))

        assertNotNull(repo.hentPerson("1234"))
        assertEquals(1, repo.hentHistorikk("1234").size)
    }

    @Test
    internal fun `mediator henter person ved endringer`() {
        val repo = HashmapRepository()

        val person = Person("1234")
        repo.lagrePerson(person)

        val mediator = PersonMediator(
                personRepository = repo
        )

        mediator.håndterNySøknad(nySøknad(
                aktørId = "1234"
        ))

        assertNotNull(repo.hentPerson("1234"))
        assertEquals(2, repo.hentHistorikk("1234").size)
    }

}


