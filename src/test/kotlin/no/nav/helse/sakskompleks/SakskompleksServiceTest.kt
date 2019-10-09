package no.nav.helse.sakskompleks

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestConstants.søknad
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.junit.jupiter.api.Test

internal class SakskompleksServiceTest {

    @Test
    fun `skal håndtere feil ved søknad uten virksomhetsnummer for arbeidsgiver`() {
        val probe = mockk<SakskompleksProbe>(relaxed = true)
        val sakskompleksService = SakskompleksService(
                sakskompleksProbe = probe,
                behovProducer = mockk(),
                sakskompleksDao = mockk())

        sakskompleksService.håndterSøknad(søknad(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<Sykepengesøknad>())
        }
    }
}
