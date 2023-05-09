package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

internal class UgyldigDataTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `ny søknad - tom er før fom`() {
        assertDoesNotThrow { sendNySøknad(SoknadsperiodeDTO(fom = 2.januar, tom = 1.januar, sykmeldingsgrad = 100)) }
    }

    @Test
    fun `sendt søknad - tom er før fom`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 2.januar, sykmeldingsgrad = 100))
        assertDoesNotThrow { sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 2.januar, tom = 1.januar, sykmeldingsgrad = 100))
        ) }
    }
}
