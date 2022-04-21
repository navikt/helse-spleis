package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtsettOppgaveTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender ut utsett_oppgave event`() = Toggle.NyTilstandsflyt.enable {
        sendNySøknad(SoknadsperiodeDTO(1.januar, 31.januar, 100))
        val (inntektsmeldingId, _) = sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        assertEquals(inntektsmeldingId.toString(), testRapid.inspektør.siste("utsett_oppgave")["hendelse"].asText())
    }
}