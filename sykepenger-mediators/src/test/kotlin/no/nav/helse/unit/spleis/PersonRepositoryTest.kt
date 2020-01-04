package no.nav.helse.unit.spleis

import io.mockk.mockk
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.spleis.PersonMediator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class PersonRepositoryTest {

    private val aktørId = "1234"
    private val nySøknad = nySøknadHendelse(
        aktørId = aktørId
    )
    private val sendtSøknad = sendtSøknadHendelse(
        aktørId = aktørId
    )

    @Test
    internal fun `mediator lagrer person ved endringer`() {
        val repo = HashmapPersonRepository()
        val mediator = PersonMediator(
                personRepository = repo,
                lagrePersonDao = repo,
                utbetalingsreferanseRepository = mockk(relaxed = true),
                lagreUtbetalingDao = mockk(relaxed = true),
                producer = mockk(relaxed = true)
        )

        mediator.onNySøknad(nySøknad)

        assertNotNull(repo.hentPerson(aktørId))
        assertEquals(1, repo.hentHistorikk(aktørId).size)
    }

    @Test
    internal fun `gitt at en ny person er lagret i databasen, og endrer tilstand, så lagres ny versjon`() {
        val repo = HashmapPersonRepository()
        val mediator = PersonMediator(
                personRepository = repo,
                lagrePersonDao = repo,
                utbetalingsreferanseRepository = mockk(relaxed = true),
                lagreUtbetalingDao = mockk(relaxed = true),
                producer = mockk(relaxed = true)
        )
        mediator.onNySøknad(nySøknad)


        val personEtterNySøknad = repo.hentPerson(aktørId)
        assertNotNull(personEtterNySøknad)
        assertEquals(1, repo.hentHistorikk(aktørId).size)

        mediator.onSendtSøknad(sendtSøknad)

        val personEtterSøknad = repo.hentPerson(aktørId)
        assertNotNull(personEtterSøknad)
        assertEquals(2, repo.hentHistorikk(aktørId).size)

    }
}


