package no.nav.helse.unit.person

import io.mockk.mockk
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.person.PersonMediator
import no.nav.helse.person.domain.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class PersonRepositoryTest {



    @Test
    internal fun `mediator lagrer person ved endringer`() {
        val repo = HashmapPersonRepository()
        val mediator = PersonMediator(
                personRepository = repo,
                behovProducer = mockk()
        )

        mediator.håndterNySøknad(nySøknad(
                aktørId = "1234"
        ))

        assertNotNull(repo.hentPerson("1234"))
        assertEquals(1, repo.hentHistorikk("1234").size)
    }

    @Test
    internal fun `mediator henter person ved endringer`() {
        val repo = HashmapPersonRepository()

        val person = Person("1234")
        repo.lagrePerson(person)

        val mediator = PersonMediator(
                personRepository = repo,
                behovProducer = mockk()
        )

        mediator.håndterNySøknad(nySøknad(
                aktørId = "1234"
        ))

        assertNotNull(repo.hentPerson("1234"))
        assertEquals(2, repo.hentHistorikk("1234").size)
    }

}


