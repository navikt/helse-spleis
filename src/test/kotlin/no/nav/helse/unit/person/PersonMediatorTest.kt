package no.nav.helse.unit.person

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.behov.BehovProducer
import no.nav.helse.hendelse.Sykepengesøknad
import no.nav.helse.person.PersonMediator
import no.nav.helse.person.PersonRepository
import no.nav.helse.sakskompleks.SakskompleksProbe
import org.junit.jupiter.api.Test

internal class PersonMediatorTest {

    @Test
    fun `skal håndtere feil ved søknad uten virksomhetsnummer for arbeidsgiver`() {
        val probe = mockk<SakskompleksProbe>(relaxed = true)
        val personRepo = mockk<PersonRepository>()
        val behovProducer = mockk<BehovProducer>()
        val sakskompleksService = PersonMediator(
                sakskompleksProbe = probe,
                personRepository = personRepo,
                behovProducer = behovProducer)

        every {
            personRepo.hentPerson(any())
        } returns null

        sakskompleksService.håndterNySøknad(nySøknad(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<Sykepengesøknad>())
        }
    }
}
