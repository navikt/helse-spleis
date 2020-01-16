package no.nav.helse.unit.spleis

import io.mockk.mockk
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.person.Problemer
import no.nav.helse.spleis.PersonMediator
import no.nav.helse.toJson
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class PersonRepositoryTest {

    private val aktørId = "1234"
    private val problemer = Problemer()
    private val nySøknad = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = "fnr",
        aktørId = aktørId,
        orgnummer = "orgnr",
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(
            Triple(LocalDate.now(), LocalDate.now(), 100)
        ),
        problemer = problemer,
        originalJson = søknadDTO(
            aktørId = aktørId,
            status = SoknadsstatusDTO.NY
        ).toJson()
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

        mediator.onNySøknad(nySøknad, problemer)

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
        mediator.onNySøknad(nySøknad, problemer)


        val personEtterNySøknad = repo.hentPerson(aktørId)
        assertNotNull(personEtterNySøknad)
        assertEquals(1, repo.hentHistorikk(aktørId).size)

        mediator.onSendtSøknad(sendtSøknad)

        val personEtterSøknad = repo.hentPerson(aktørId)
        assertNotNull(personEtterSøknad)
        assertEquals(2, repo.hentHistorikk(aktørId).size)

    }
}


