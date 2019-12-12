package no.nav.helse.unit.spleis

import io.mockk.mockk
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.spleis.SakMediator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class SakRepositoryTest {



    @Test
    internal fun `mediator lagrer sak ved endringer`() {
        val repo = HashmapSakRepository()
        val mediator = SakMediator(
                sakRepository = repo,
                lagreSakDao = repo,
                utbetalingsreferanseRepository = mockk(relaxed = true),
                lagreUtbetalingDao = mockk(relaxed = true),
                producer = mockk(relaxed = true)
        )

        mediator.håndter(nySøknadHendelse(
                aktørId = "1234"
        ))

        assertNotNull(repo.hentSak("1234"))
        assertEquals(1, repo.hentHistorikk("1234").size)
    }

    @Test
    internal fun `gitt at en ny sak er lagret i databasen, og endrer tilstand, så lagres ny versjon`() {
        val repo = HashmapSakRepository()
        val aktørId = "1234"
        val mediator = SakMediator(
                sakRepository = repo,
                lagreSakDao = repo,
                utbetalingsreferanseRepository = mockk(relaxed = true),
                lagreUtbetalingDao = mockk(relaxed = true),
                producer = mockk(relaxed = true)
        )
        mediator.håndter(nySøknadHendelse(
                aktørId = aktørId
        ))


        val sakEtterNySøknad = repo.hentSak(aktørId)
        assertNotNull(sakEtterNySøknad)
        assertEquals(1, repo.hentHistorikk(aktørId).size)

        mediator.håndter(sendtSøknadHendelse(
                aktørId = aktørId
        ))

        val sakEtterSøknad = repo.hentSak(aktørId)
        assertNotNull(sakEtterSøknad)
        assertEquals(2, repo.hentHistorikk(aktørId).size)

    }
}


