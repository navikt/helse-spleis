package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import org.junit.jupiter.api.Test

internal class FrilanserTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `frilansersøknad`() {
        sendNySøknadFrilanser(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)
        )
        sendFrilanssøknad(
            perioder =
                listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        assertTilstander(0, "TIL_INFOTRYGD")
    }
}
