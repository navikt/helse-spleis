package no.nav.helse.unit.person

import io.mockk.mockk
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
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
                lagrePersonDao = repo,
                behovProducer = mockk()
        )

        mediator.håndterNySøknad(nySøknad(
                aktørId = "1234"
        ))

        assertNotNull(repo.hentPerson("1234"))
        assertEquals(1, repo.hentHistorikk("1234").size)
    }

    @Test
    internal fun `gitt at en ny person er lagret i databasen, og endrer tilstand, så lagres ny versjon`() {
        val repo = HashmapPersonRepository()
        val aktørId = "1234"
        val mediator = PersonMediator(
                personRepository = repo,
                lagrePersonDao = repo,
                behovProducer = mockk()
        )
        mediator.håndterNySøknad(nySøknad(
                aktørId = aktørId
        ))


        val personEtterNySøknad = repo.hentPerson(aktørId)
        assertNotNull(personEtterNySøknad)
        assertEquals(1, repo.hentHistorikk(aktørId).size)

        mediator.håndterSendtSøknad(sendtSøknad(
                aktørId = aktørId
        ))

        val personEtterSøknad = repo.hentPerson(aktørId)
        assertNotNull(personEtterSøknad)
        assertEquals(2, repo.hentHistorikk(aktørId).size)

    }
}


