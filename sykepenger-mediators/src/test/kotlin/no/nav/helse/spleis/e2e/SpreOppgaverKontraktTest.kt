package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpreOppgaverKontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `inntektmelding før søknad`() {
        sendNySøknad(SoknadsperiodeDTO(1.januar, 31.januar, 100))
        val (inntektsmeldingId, _) = sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)
        val inntektsmeldingFørSøknad = testRapid.inspektør.siste("inntektsmelding_før_søknad")
        assertTrue(inntektsmeldingFørSøknad is ObjectNode)
        assertInntektsmeldingFørSøknad(inntektsmeldingFørSøknad, inntektsmeldingId)
    }

    private fun assertInntektsmeldingFørSøknad(melding: JsonNode, inntektsmeldingId: UUID) {
        assertTrue(melding.path("inntektsmeldingId").asText().isNotEmpty())
        assertTrue(melding.path("organisasjonsnummer").asText().isNotEmpty())
        melding.path("overlappendeSykmeldingsperioder").forEach { periode ->
            assertDato(periode.path("fom").asText())
            assertDato(periode.path("tom").asText())
        }
    }


    private fun assertDato(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDate.parse(tekst) }
    }
}


