package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GenerellMeldingskontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtaksperiode endret`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        val vedtaksperiodeEndret = testRapid.inspektør.siste("vedtaksperiode_endret")
        assertVedtaksperiodeEndret(vedtaksperiodeEndret, søknadId, "sendt_søknad_nav")
    }

    @Test
    fun `behov`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        val (meldingId, _) = sendInntektsmelding(
            listOf(Periode(fom = 3.januar, tom = 18.januar)),
            førsteFraværsdag = 3.januar
        )
        val behov = testRapid.inspektør.siste("behov")
        assertBehov(behov, meldingId, "inntektsmelding")
    }

    @Test
    fun `replay inntektsmelding`() {
        val (meldingId, _) = sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        val behov = testRapid.inspektør.siste("behov")
        assertBehov(behov, meldingId, "inntektsmelding_replay")
    }

    private fun assertVedtaksperiodeEndret(melding: JsonNode, originalMeldingId: UUID, originalMeldingtype: String) {
        assertStandardinformasjon(melding)
        assertSporingsinformasjon(melding, originalMeldingId, originalMeldingtype)
        assertTrue(melding.path("gjeldendeTilstand").asText().isNotEmpty())
        assertTrue(melding.path("forrigeTilstand").asText().isNotEmpty())
        assertTrue(melding.path("aktivitetslogg").isObject)
        assertTrue(melding.path("aktivitetslogg").path("aktiviteter").isArray)
        assertTrue(melding.path("aktivitetslogg").path("kontekster").isArray)
        assertTrue(melding.path("harVedtaksperiodeWarnings").isBoolean)
        assertTrue(melding.path("hendelser").isArray)
        assertDatotid(melding.path("makstid").asText())
    }

    private fun assertBehov(melding: JsonNode, originalMeldingId: UUID, originalMeldingtype: String) {
        assertStandardinformasjon(melding)
        assertSporingsinformasjon(melding, originalMeldingId, originalMeldingtype)
        assertTrue(melding.path("@behovId").asText().isNotEmpty())
        assertTrue(melding.path("@behov").isArray)
    }

    private fun assertStandardinformasjon(melding: JsonNode) {
        assertTrue(melding.path("@id").asText().isNotEmpty())
        assertDatotid(melding.path("@opprettet").asText())
    }

    private fun assertSporingsinformasjon(melding: JsonNode, originalMeldingId: UUID, originalMeldingtype: String) {
        assertEquals(originalMeldingtype, melding.path("@forårsaket_av").path("event_name").asText())
        assertEquals(originalMeldingId.toString(), melding.path("@forårsaket_av").path("id").asText())
    }

    private fun assertDato(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDate.parse(tekst) }
    }

    private fun assertDatotid(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDateTime.parse(tekst) }
    }
}


