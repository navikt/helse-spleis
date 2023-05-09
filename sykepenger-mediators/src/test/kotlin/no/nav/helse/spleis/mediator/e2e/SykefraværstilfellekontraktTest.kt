package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SykefraværstilfellekontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sykefraværstilfeller`() {
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), orgnummer = "a1")
        val søknadId = sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), orgnummer = "a2")
        val sykefraværstilfeller = testRapid.inspektør.meldinger("sykefraværstilfeller")
        assertEquals(2, sykefraværstilfeller.size)
        val sisteSykefraværstilfelle = sykefraværstilfeller.last()
        assertSykefraværstilfelle(sisteSykefraværstilfelle, søknadId, "sendt_søknad_nav")
    }

    private fun assertSykefraværstilfelle(melding: JsonNode, originalMeldingId: UUID, originalMeldingtype: String) {
        assertStandardinformasjon(melding)
        assertSporingsinformasjon(melding, originalMeldingId, originalMeldingtype)
        assertTrue(melding.path("tilfeller").isArray)
        melding.path("tilfeller").first().also { tilfelle ->
            assertDato(tilfelle.path("dato").asText())
            assertTrue(tilfelle.path("perioder").isArray)
            tilfelle.path("perioder").first().also { periode ->
                assertTrue(periode.path("vedtaksperiodeId").asText().isNotEmpty())
                assertTrue(periode.path("organisasjonsnummer").asText().isNotEmpty())
                assertDato(periode.path("fom").asText())
                assertDato(periode.path("tom").asText())
            }
        }
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


