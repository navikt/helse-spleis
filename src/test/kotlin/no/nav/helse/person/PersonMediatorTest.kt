package no.nav.helse.person

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.hendelse.Sykepengesøknad
import no.nav.helse.sakskompleks.SakskompleksProbe
import org.junit.jupiter.api.Test

internal class PersonMediatorTest {

    @Test
    fun `skal håndtere feil ved søknad uten virksomhetsnummer for arbeidsgiver`() {
        val probe = mockk<SakskompleksProbe>(relaxed = true)
        val sakskompleksService = PersonMediator(
                sakskompleksProbe = probe,
                behovProducer = mockk(),
                personRepository = mockk())

        sakskompleksService.håndterNySøknad(nySøknad(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<Sykepengesøknad>())
        }
    }
}
