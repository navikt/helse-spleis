package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpreOppgaverKontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `inntektmelding før søknad`() {
        sendNySøknad(SoknadsperiodeDTO(1.januar, 31.januar, 100))
        val (inntektsmeldingId, _) = sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)
        val inntektsmeldingFørSøknad = testRapid.inspektør.siste("inntektsmelding_før_søknad")
        assertTrue(inntektsmeldingFørSøknad is ObjectNode)
        assertInntektsmeldingFørSøknad(inntektsmeldingFørSøknad)
    }

    @Test
    fun `inntektmelding ikke håndtert`() {
        sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)
        val melding = testRapid.inspektør.siste("inntektsmelding_ikke_håndtert")
        assertInntektsmeldingIkkeHåndtert(melding)
    }

    @Test
    fun `Sender ut inntektsmeldingHåndtertEvent når en vedtaksperiode har håndtert en inntektsmelding`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        )
        val (inntektsmeldingId, _) = sendInntektsmelding(
            listOf(Periode(fom = 1.januar, tom = 16.januar)),
            førsteFraværsdag = 1.januar
        )
        val vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(0)

        val meldinger = testRapid.inspektør.meldinger("inntektsmelding_håndtert")
        assertEquals(1, meldinger.size)
        meldinger[0].also { inntektsmeldingHåndtertEvent ->
            // håndtert inntekten fra im
            assertEquals(inntektsmeldingId.toString(), inntektsmeldingHåndtertEvent["inntektsmeldingId"].asText())
            assertEquals(vedtaksperiodeId.toString(), inntektsmeldingHåndtertEvent["vedtaksperiodeId"].asText())
            assertNotNull(inntektsmeldingHåndtertEvent["@opprettet"].asLocalDateTime())
        }
    }

    private fun assertInntektsmeldingFørSøknad(melding: JsonNode) {
        assertTrue(melding.path("inntektsmeldingId").asText().isNotEmpty())
        assertTrue(melding.path("organisasjonsnummer").asText().isNotEmpty())
        melding.path("overlappendeSykmeldingsperioder").forEach { periode ->
            assertDato(periode.path("fom").asText())
            assertDato(periode.path("tom").asText())
        }
    }

    private fun assertInntektsmeldingIkkeHåndtert(melding: JsonNode) {
        assertTrue(melding.path("inntektsmeldingId").asText().isNotEmpty())
        assertTrue(melding.path("organisasjonsnummer").asText().isNotEmpty())
    }


    private fun assertDato(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDate.parse(tekst) }
    }
}


