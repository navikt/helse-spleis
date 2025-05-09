package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class PåminnelserTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `påminnelse når person ikke finnes`() {
        sendNyPåminnelse()
        assertEquals(0, testRapid.inspektør.antall())
    }

    @Test
    fun `påminnelse for feil tilstand`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendNyPåminnelse(0)
        val vedtaksperiodeIkkePåminnet = testRapid.inspektør.meldinger("vedtaksperiode_ikke_påminnet").single()
        assertEquals("vedtaksperiode_ikke_påminnet", vedtaksperiodeIkkePåminnet.path("@event_name").asText())
        assertEquals("AVVENTER_INNTEKTSMELDING", vedtaksperiodeIkkePåminnet.path("tilstand").asText())
        assertVedtaksperiodeIkkePåminnet(vedtaksperiodeIkkePåminnet)
    }

    @Test
    fun `påminnelse for riktig tilstand`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendNyPåminnelse(0, tilstandType = TilstandType.AVVENTER_INNTEKTSMELDING)
        val vedtaksperiodePåminnet = testRapid.inspektør.meldinger("vedtaksperiode_påminnet").single()
        assertEquals("vedtaksperiode_påminnet", vedtaksperiodePåminnet.path("@event_name").asText())
        assertEquals("AVVENTER_INNTEKTSMELDING", vedtaksperiodePåminnet.path("tilstand").asText())
        assertVedtaksperiodePåminnet(vedtaksperiodePåminnet)
    }

    @Test
    fun utbetalingpåminnelse() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendNyUtbetalingpåminnelse(0, Utbetalingstatus.OVERFØRT)
        assertUtbetalingTilstander(0, "NY", "IKKE_UTBETALT", "OVERFØRT")
        assertEquals(2, (0 until testRapid.inspektør.antall()).filter { "Utbetaling" in testRapid.inspektør.melding(it).path("@behov").map(JsonNode::asText) }.size)
    }

    private fun assertVedtaksperiodePåminnet(melding: JsonNode) {
        assertTrue(melding.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(melding.path("organisasjonsnummer").asText().isNotEmpty())
        assertTrue(melding.path("vedtaksperiodeId").asText().isNotEmpty())
        assertTrue(melding.path("tilstand").asText().isNotEmpty())
        assertTrue(melding.path("antallGangerPåminnet").asText().isNotEmpty())
        assertDoesNotThrow { melding.path("tilstandsendringstidspunkt").asLocalDateTime() }
        assertDoesNotThrow { melding.path("påminnelsestidspunkt").asLocalDateTime() }
        assertDoesNotThrow { melding.path("nestePåminnelsestidspunkt").asLocalDateTime() }
    }

    private fun assertVedtaksperiodeIkkePåminnet(melding: JsonNode) {
        assertTrue(melding.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(melding.path("organisasjonsnummer").asText().isNotEmpty())
        assertTrue(melding.path("vedtaksperiodeId").asText().isNotEmpty())
        assertTrue(melding.path("tilstand").asText().isNotEmpty())
    }

    private fun assertVedtaksperiodeIkkeFunnet(melding: JsonNode) {
        assertTrue(melding.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(melding.path("organisasjonsnummer").asText().isNotEmpty())
        assertTrue(melding.path("vedtaksperiodeId").asText().isNotEmpty())
    }
}


