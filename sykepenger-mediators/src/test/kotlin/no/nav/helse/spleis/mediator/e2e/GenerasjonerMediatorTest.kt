package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GenerasjonerMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtak iverksatt`() {
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        val generasjonOpprettet = testRapid.inspektør.meldinger("generasjon_opprettet").single()
        val generasjonOpprettetIndeks = testRapid.inspektør.indeksFor(generasjonOpprettet)
        val generasjonLukket = testRapid.inspektør.meldinger("generasjon_lukket").single()
        val generasjonLukketIndeks = testRapid.inspektør.indeksFor(generasjonLukket)
        val generasjonAvsluttet = testRapid.inspektør.meldinger("avsluttet_med_vedtak").single()
        val generasjonAvsluttetIndeks = testRapid.inspektør.indeksFor(generasjonAvsluttet)

        assertTrue(generasjonOpprettetIndeks < generasjonLukketIndeks) { "generasjon_opprettet må sendes først" }
        assertTrue(generasjonLukketIndeks < generasjonAvsluttetIndeks) { "generasjon_lukket bør sendes før generasjon avsluttes" }
        verifiserGenerasjonLukketKontrakt(generasjonLukket)
    }

    @Test
    fun `vedtak fattet`() {
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)

        val generasjonOpprettet = testRapid.inspektør.meldinger("generasjon_opprettet").single()
        val generasjonOpprettetIndeks = testRapid.inspektør.indeksFor(generasjonOpprettet)
        val generasjonLukket = testRapid.inspektør.meldinger("generasjon_lukket").single()
        val generasjonLukketIndeks = testRapid.inspektør.indeksFor(generasjonLukket)
        val generasjonAvsluttet = testRapid.inspektør.meldinger("avsluttet_med_vedtak")

        assertTrue(generasjonOpprettetIndeks < generasjonLukketIndeks) { "generasjon_opprettet må sendes først" }
        assertEquals(emptyList<Any>(), generasjonAvsluttet)
        verifiserGenerasjonLukketKontrakt(generasjonLukket)
    }

    @Test
    fun `avsluttet uten utbetaling`() {
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 10.januar, sykmeldingsgrad = 100)))

        val generasjonOpprettet = testRapid.inspektør.meldinger("generasjon_opprettet").single()
        val generasjonOpprettetIndeks = testRapid.inspektør.indeksFor(generasjonOpprettet)
        val generasjonLukket = testRapid.inspektør.meldinger("generasjon_lukket").single()
        val generasjonLukketIndeks = testRapid.inspektør.indeksFor(generasjonLukket)
        val generasjonAvsluttet = testRapid.inspektør.meldinger("avsluttet_uten_vedtak").single()
        val generasjonAvsluttetIndeks = testRapid.inspektør.indeksFor(generasjonAvsluttet)

        assertTrue(generasjonOpprettetIndeks < generasjonLukketIndeks) { "generasjon_opprettet må sendes først" }
        assertTrue(generasjonLukketIndeks < generasjonAvsluttetIndeks) { "generasjon_lukket bør sendes før generasjon avsluttes" }
        verifiserGenerasjonLukketKontrakt(generasjonLukket)
    }

    @Test
    fun `vedtak annuleres`() {
        nyttVedtak(1.januar, 31.januar)
        val fagsystemId = testRapid.inspektør.siste("utbetaling_utbetalt").path("arbeidsgiverOppdrag").path("fagsystemId").asText()
        sendAnnullering(fagsystemId)

        val generasjonOpprettet = testRapid.inspektør.meldinger("generasjon_opprettet")
        val førsteGenerasjonOpprettetIndeks = testRapid.inspektør.indeksFor(generasjonOpprettet.first())
        val sisteGenerasjonOpprettetIndeks = testRapid.inspektør.indeksFor(generasjonOpprettet.last())
        val generasjonLukket = testRapid.inspektør.meldinger("generasjon_lukket").single()
        val generasjonLukketIndeks = testRapid.inspektør.indeksFor(generasjonLukket)
        val generasjonForkastet = testRapid.inspektør.meldinger("generasjon_forkastet").single()
        val generasjonForkastetIndeks = testRapid.inspektør.indeksFor(generasjonForkastet)

        assertEquals(2, generasjonOpprettet.size) { "forventer to generasjoner" }
        assertTrue(førsteGenerasjonOpprettetIndeks < generasjonLukketIndeks) { "generasjon_opprettet må sendes først" }
        assertTrue(sisteGenerasjonOpprettetIndeks > generasjonLukketIndeks) { "det skal ikke sendes  generasjon_lukket for forkastede generasjoner" }
        assertTrue(sisteGenerasjonOpprettetIndeks < generasjonForkastetIndeks) { "generasjon_forkastet må sendes etter generasjon_opprettet" }
        verifiserGenerasjonForkastetKontrakt(generasjonForkastet)
    }

    private fun verifiserGenerasjonLukketKontrakt(generasjonLukket: JsonNode) {
        assertEquals("generasjon_lukket", generasjonLukket.path("@event_name").asText())
        assertTrue(generasjonLukket.path("fødselsnummer").isTextual)
        assertTrue(generasjonLukket.path("organisasjonsnummer").isTextual)
        assertTrue(generasjonLukket.path("vedtaksperiodeId").isTextual)
        assertTrue(generasjonLukket.path("generasjonId").isTextual)
    }

    private fun verifiserGenerasjonForkastetKontrakt(generasjonLukket: JsonNode) {
        assertEquals("generasjon_forkastet", generasjonLukket.path("@event_name").asText())
        assertTrue(generasjonLukket.path("fødselsnummer").isTextual)
        assertTrue(generasjonLukket.path("organisasjonsnummer").isTextual)
        assertTrue(generasjonLukket.path("vedtaksperiodeId").isTextual)
        assertTrue(generasjonLukket.path("generasjonId").isTextual)
    }
}
