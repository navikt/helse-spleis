package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RevurderingseventyrkontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `eventyr`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling(utbetalingOK = true)

        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        )
        sendYtelser(1)
        sendSimulering(1, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(1, true)
        sendUtbetaling(utbetalingOK = true)

        sendOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger = listOf(
                TestMessageFactory.Arbeidsgiveropplysning(
                    organisasjonsnummer = ORGNUMMER,
                    månedligInntekt = 20000.0,
                    forklaring = "forklaring",
                    subsumsjon = null,
                    refusjonsopplysninger = listOf(
                        TestMessageFactory.Refusjonsopplysning(
                            fom = 1.januar,
                            tom = null,
                            beløp = 20000.0
                        )
                    )
                )
            )
        )

        val eventyr = testRapid.inspektør.siste("overstyring_igangsatt")
        assertRevurderingIgangsatt(eventyr)
    }

    private fun assertRevurderingIgangsatt(eventyr: JsonNode) {
        val hendelseId = eventyr.path("@id").asText()
        val id = eventyr.path("revurderingId").asText()
        val kilde = eventyr.path("kilde").asText()
        assertTrue(eventyr.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(eventyr.path("aktørId").asText().isNotEmpty())
        assertDatotid(eventyr.path("@opprettet").asText())
        assertTrue(hendelseId.isNotEmpty())
        assertDoesNotThrow { UUID.fromString(hendelseId) }
        assertTrue(id.isNotEmpty())
        assertDoesNotThrow { UUID.fromString(id) }
        assertTrue(kilde.isNotEmpty())
        assertDoesNotThrow { UUID.fromString(kilde) }
        assertDato(eventyr.path("skjæringstidspunkt").asText())
        assertDato(eventyr.path("periodeForEndringFom").asText())
        assertDato(eventyr.path("periodeForEndringTom").asText())
        assertTrue(eventyr.path("årsak").asText().isNotEmpty())
        assertTrue(eventyr.path("typeEndring").asText().isNotEmpty())
        assertTrue(eventyr.path("berørtePerioder").isArray)
        eventyr.path("berørtePerioder").forEach { periode ->
            assertDato(periode.path("skjæringstidspunkt").asText())
            assertDato(periode.path("periodeFom").asText())
            assertDato(periode.path("periodeTom").asText())
            assertTrue(periode.path("typeEndring").asText().isNotEmpty())
            assertTrue(periode.path("orgnummer").asText().isNotEmpty())
            val vedtaksperiodeId = periode.path("vedtaksperiodeId").asText()
            assertDoesNotThrow { UUID.fromString(vedtaksperiodeId) }
        }
    }

    private fun assertDatotid(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDateTime.parse(tekst) }
    }

    private fun assertDato(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDate.parse(tekst) }
    }

}


