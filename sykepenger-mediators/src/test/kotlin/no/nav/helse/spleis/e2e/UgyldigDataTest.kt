package no.nav.helse.spleis.e2e

import no.nav.helse.januar
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
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
        assertDoesNotThrow { sendSøknad(listOf(SoknadsperiodeDTO(fom = 2.januar, tom = 1.januar, sykmeldingsgrad = 100))) }
    }
}
