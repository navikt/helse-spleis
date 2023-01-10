package no.nav.helse.spleis.e2e

import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtsettOppgaveTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender ut utsett_oppgave event for inntektsmelding`() {
        sendNySøknad(SoknadsperiodeDTO(1.januar, 31.januar, 100))
        val (inntektsmeldingId, _) = sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        assertEquals(inntektsmeldingId.toString(), testRapid.inspektør.siste("utsett_oppgave")["hendelse"].asText())
    }

    @Test
    fun `sender ut utsett_oppgave event for korrigert søknad`() {
        sendNySøknad(SoknadsperiodeDTO(1.januar, 31.januar, 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        val søknadId2 = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 80))
        )

        assertEquals(søknadId2.toString(), testRapid.inspektør.siste("utsett_oppgave")["hendelse"].asText())
    }
}