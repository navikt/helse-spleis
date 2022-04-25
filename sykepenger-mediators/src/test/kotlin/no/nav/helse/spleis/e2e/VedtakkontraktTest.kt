package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.januar
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.FravarDTO
import no.nav.syfo.kafka.felles.FravarstypeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VedtakkontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtak med utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        assertVedtakMedUtbetaling()
    }

    @Test
    fun `vedtak uten utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            fravær = listOf(FravarDTO(19.januar, 26.januar, FravarstypeDTO.FERIE))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        assertVedtakUtenUtbetaling()
    }

    @Test
    fun `vedtak med utbetaling uten utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendNySøknad(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            fravær = listOf(FravarDTO(fom = 27.januar, tom = 31.januar, FravarstypeDTO.FERIE))
        )
        sendYtelserUtenSykepengehistorikk(1)
        sendUtbetalingsgodkjenning(1)
        assertVedtakMedUtbetalingUtenUtbetaling()
    }

    private fun assertVedtakMedUtbetaling() {
        testRapid.inspektør.siste("vedtak_fattet").also { melding ->
            assertVedtak(melding)
            assertTrue(melding.path("utbetalingId").asText().isNotEmpty())
        }
    }

    private fun assertVedtakMedUtbetalingUtenUtbetaling() {
        testRapid.inspektør.siste("vedtak_fattet").also { melding ->
            assertVedtak(melding)
            assertFalse(melding.path("utbetalingId").isMissingOrNull())
            assertEquals(melding.path("utbetalingId").asText(), testRapid.inspektør.siste("utbetaling_uten_utbetaling").path("utbetalingId").asText())
        }
    }

    private fun assertVedtakUtenUtbetaling() {
        testRapid.inspektør.siste("vedtak_fattet").also { melding ->
            assertVedtak(melding)
            assertTrue(melding.path("utbetalingId").isMissingOrNull())
        }
    }

    private fun assertVedtak(melding: JsonNode) {
        assertTrue(melding.path("vedtaksperiodeId").asText().isNotEmpty())
        assertTrue(melding.path("hendelser").isArray)
        assertDato(melding.path("fom").asText())
        assertDato(melding.path("tom").asText())
        assertDato(melding.path("skjæringstidspunkt").asText())
        assertTall(melding, "sykepengegrunnlag")
        assertTall(melding, "inntekt")
    }

    private fun assertDato(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDate.parse(tekst) }
    }

    private fun assertTall(melding: JsonNode, key: String) {
        assertTrue(melding.path(key).isDouble)
        assertTrue(melding.path(key).doubleValue() >= 0)
    }

    private fun assert(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDateTime.parse(tekst) }
    }
}


