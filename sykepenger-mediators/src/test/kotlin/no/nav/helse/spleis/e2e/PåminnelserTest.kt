package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.januar
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PåminnelserTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `påminnelse når person ikke finnes`() {
        sendNyPåminnelse()
        assertEquals(0, testRapid.inspektør.antall())
    }

    @Test
    fun `påminnelse når vedtaksperiode ikke finnes`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        val id = sendNyPåminnelse()
        assertEquals(4, testRapid.inspektør.antall())
        val melding = testRapid.inspektør.melding(3)
        assertEquals("vedtaksperiode_ikke_funnet", melding.path("@event_name").asText())
        assertEquals("$id", melding.path("vedtaksperiodeId").asText())
    }

    @Test
    fun `påminnelse for feil tilstand`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendNyPåminnelse(0)
        assertEquals("vedtaksperiode_ikke_påminnet", testRapid.inspektør.melding(3).path("@event_name").asText())
        assertEquals("AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", testRapid.inspektør.melding(3).path("tilstand").asText())
    }

    @Test
    fun `utbetalingpåminnelse`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendNyUtbetalingpåminnelse(0, Utbetalingstatus.SENDT)
        assertUtbetalingTilstander(0, "NY", "IKKE_UTBETALT", "GODKJENT", "SENDT")
        assertEquals(2, (0 until testRapid.inspektør.antall()).filter { "Utbetaling" in testRapid.inspektør.melding(it).path("@behov").map(JsonNode::asText) }.size)
    }
}


