package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BehandlingerMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtak iverksatt`() {
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        val behandlingOpprettet = testRapid.inspektør.meldinger("behandling_opprettet").single()
        val behandlingOpprettetIndeks = testRapid.inspektør.indeksFor(behandlingOpprettet)
        val behandlingLukket = testRapid.inspektør.meldinger("behandling_lukket").single()
        val behandlingLukketIndeks = testRapid.inspektør.indeksFor(behandlingLukket)
        val behandlingAvsluttet = testRapid.inspektør.meldinger("avsluttet_med_vedtak").single()
        val behandlingAvsluttetIndeks = testRapid.inspektør.indeksFor(behandlingAvsluttet)

        assertTrue(behandlingOpprettetIndeks < behandlingLukketIndeks) { "behandling_opprettet må sendes først" }
        assertTrue(behandlingLukketIndeks < behandlingAvsluttetIndeks) { "behandling_lukket bør sendes før behandling avsluttes" }

        verifiserBehandlingOpprettetKontrakt(behandlingOpprettet)
        verifiserBehandlingLukketKontrakt(behandlingLukket)
    }

    @Test
    fun `vedtak iverksatt selvstendig`() {
        sendSelvstendigsøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
            ventetid = 1.januar til 16.januar
        )
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()

        val behandlingOpprettet = testRapid.inspektør.meldinger("behandling_opprettet").single()
        val behandlingOpprettetIndeks = testRapid.inspektør.indeksFor(behandlingOpprettet)
        val behandlingLukket = testRapid.inspektør.meldinger("behandling_lukket").single()
        val behandlingLukketIndeks = testRapid.inspektør.indeksFor(behandlingLukket)
        val behandlingAvsluttet = testRapid.inspektør.meldinger("avsluttet_med_vedtak").single()
        val behandlingAvsluttetIndeks = testRapid.inspektør.indeksFor(behandlingAvsluttet)

        assertTrue(behandlingOpprettetIndeks < behandlingLukketIndeks) { "behandling_opprettet må sendes først" }
        assertTrue(behandlingLukketIndeks < behandlingAvsluttetIndeks) { "behandling_lukket bør sendes før behandling avsluttes" }

        verifiserBehandlingOpprettetKontrakt(behandlingOpprettet)
        verifiserBehandlingLukketKontrakt(behandlingLukket)
    }

    @Test
    fun `vedtak fattet`() {
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)

        val behandlingOpprettet = testRapid.inspektør.meldinger("behandling_opprettet").single()
        val behandlingOpprettetIndeks = testRapid.inspektør.indeksFor(behandlingOpprettet)
        val behandlingLukket = testRapid.inspektør.meldinger("behandling_lukket").single()
        val behandlingLukketIndeks = testRapid.inspektør.indeksFor(behandlingLukket)
        val behandlingAvsluttet = testRapid.inspektør.meldinger("avsluttet_med_vedtak")

        assertTrue(behandlingOpprettetIndeks < behandlingLukketIndeks) { "behandling_opprettet må sendes først" }
        assertEquals(emptyList<Any>(), behandlingAvsluttet)
        verifiserBehandlingLukketKontrakt(behandlingLukket)
    }

    @Test
    fun `avsluttet uten utbetaling`() {
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 10.januar, sykmeldingsgrad = 100)))

        val behandlingOpprettet = testRapid.inspektør.meldinger("behandling_opprettet").single()
        val behandlingOpprettetIndeks = testRapid.inspektør.indeksFor(behandlingOpprettet)
        val behandlingLukket = testRapid.inspektør.meldinger("behandling_lukket").single()
        val behandlingLukketIndeks = testRapid.inspektør.indeksFor(behandlingLukket)
        val behandlingAvsluttet = testRapid.inspektør.meldinger("avsluttet_uten_vedtak").single()
        val behandlingAvsluttetIndeks = testRapid.inspektør.indeksFor(behandlingAvsluttet)

        assertTrue(behandlingOpprettetIndeks < behandlingLukketIndeks) { "behandling_opprettet må sendes først" }
        assertTrue(behandlingLukketIndeks < behandlingAvsluttetIndeks) { "behandling_lukket bør sendes før behandling avsluttes" }
        verifiserBehandlingLukketKontrakt(behandlingLukket)
    }

    @Test
    fun `vedtak annulleres`() {
        nyttVedtak(1.januar, 31.januar)
        sendAnnullering(0)
        sendUtbetaling()

        val behandlingOpprettet = testRapid.inspektør.meldinger("behandling_opprettet")
        val førsteBehandlingOpprettetIndeks = testRapid.inspektør.indeksFor(behandlingOpprettet.first())
        val sisteBehandlingOpprettetIndeks = testRapid.inspektør.indeksFor(behandlingOpprettet.last())
        val behandlingLukket = testRapid.inspektør.meldinger("behandling_lukket").single()
        val behandlingLukketIndeks = testRapid.inspektør.indeksFor(behandlingLukket)
        val behandlingForkastet = testRapid.inspektør.meldinger("behandling_forkastet").single()
        val behandlingForkastetIndeks = testRapid.inspektør.indeksFor(behandlingForkastet)

        assertEquals(2, behandlingOpprettet.size) { "forventer to behandlinger" }
        assertTrue(førsteBehandlingOpprettetIndeks < behandlingLukketIndeks) { "behandling_opprettet må sendes først" }
        assertTrue(sisteBehandlingOpprettetIndeks > behandlingLukketIndeks) { "det skal ikke sendes behandling_lukket for forkastede behandlinger" }
        assertTrue(sisteBehandlingOpprettetIndeks < behandlingForkastetIndeks) { "behandling_forkastet må sendes etter behandling_opprettet" }
        verifiserBehandlingForkastetKontrakt(behandlingForkastet)
    }

    private fun verifiserBehandlingOpprettetKontrakt(behandlingOpprettet: JsonNode) {
        assertEquals("behandling_opprettet", behandlingOpprettet.path("@event_name").asText())
        assertTrue(behandlingOpprettet.path("fødselsnummer").isTextual)
        assertTrue(behandlingOpprettet.path("yrkesaktivitetstype").isTextual)
        assertTrue(behandlingOpprettet.path("organisasjonsnummer").takeIf { behandlingOpprettet.path("yrkesaktivitetstype").asText() == "ARBEDISTAKER" }?.isTextual ?: true)
        assertTrue(behandlingOpprettet.path("vedtaksperiodeId").isTextual)
        assertTrue(behandlingOpprettet.path("behandlingId").isTextual)
        assertDato(behandlingOpprettet.path("fom").asText())
        assertDato(behandlingOpprettet.path("tom").asText())
    }

    private fun verifiserBehandlingLukketKontrakt(behandlingLukket: JsonNode) {
        assertEquals("behandling_lukket", behandlingLukket.path("@event_name").asText())
        assertTrue(behandlingLukket.path("fødselsnummer").isTextual)
        assertTrue(behandlingLukket.path("organisasjonsnummer").takeIf { behandlingLukket.path("yrkesaktivitetstype").asText() == "ARBEDISTAKER" }?.isTextual ?: true)
        assertTrue(behandlingLukket.path("vedtaksperiodeId").isTextual)
        assertTrue(behandlingLukket.path("behandlingId").isTextual)
    }

    private fun verifiserBehandlingForkastetKontrakt(behandlingForkastet: JsonNode) {
        assertEquals("behandling_forkastet", behandlingForkastet.path("@event_name").asText())
        assertTrue(behandlingForkastet.path("fødselsnummer").isTextual)
        assertTrue(behandlingForkastet.path("organisasjonsnummer").isTextual)
        assertTrue(behandlingForkastet.path("vedtaksperiodeId").isTextual)
        assertTrue(behandlingForkastet.path("behandlingId").isTextual)
        assertTrue(behandlingForkastet.path("automatiskBehandling").isBoolean)
    }

    private fun assertDato(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDate.parse(tekst) }
    }
}
