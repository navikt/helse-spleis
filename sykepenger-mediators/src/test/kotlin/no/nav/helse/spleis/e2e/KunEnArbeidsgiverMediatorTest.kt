package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Naturalytelse
import no.nav.inntektsmeldingkontrakt.OpphoerAvNaturalytelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class KunEnArbeidsgiverMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `påminnelse for vedtaksperiode som ikke finnes`() {
        sendNyPåminnelse()
        assertEquals(1, testRapid.inspektør.antall())
        assertEquals("vedtaksperiode_ikke_funnet", testRapid.inspektør.melding(0).path("@event_name").asText())
    }

    @Test
    fun `påminnelse for feil tilstand`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendNyPåminnelse(0)
        assertEquals("vedtaksperiode_ikke_påminnet", testRapid.inspektør.melding(1).path("@event_name").asText())
        assertEquals("MOTTATT_SYKMELDING_FERDIG_GAP", testRapid.inspektør.melding(1).path("tilstand").asText())
    }

    @Test
    fun `ingen historie med Søknad først`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling(0)

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )
    }

    @Test
    fun `perioder påvirket av annullering-event blir forkastet men forblir i Avsluttet`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling(0)
        val fagsystemId = testRapid.inspektør.let { it.melding(it.antall() - 1)["utbetalt"][0]["fagsystemId"] }.asText()
        sendAnnullering(fagsystemId)

        assertForkastedeTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )
    }

    @Test
    fun `Ny, tidligere sykmelding medfører replay av første periode`() {
        Toggles.replayEnabled = true

        sendNySøknad(SoknadsperiodeDTO(fom = 2.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))

        assertForkastedeTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP")
        assertIkkeForkastedeTilstander(1, "MOTTATT_SYKMELDING_FERDIG_GAP")
        assertIkkeForkastedeTilstander(2, "MOTTATT_SYKMELDING_UFERDIG_GAP")
    }

    @Test
    fun `Inntektsmelding med første fraværsdag 1 januar skal ikke gjøre at sykmelding nr 2 ikke blir behandlet`() {
        Toggles.replayEnabled = true

        sendNySøknad(SoknadsperiodeDTO(fom = 2.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(0, perioder = listOf(SoknadsperiodeDTO(fom = 2.februar, tom = 28.februar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 2.februar)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        assertForkastedeTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_GAP", "AVVENTER_VILKÅRSPRØVING_GAP")
        assertIkkeForkastedeTilstander(1, "MOTTATT_SYKMELDING_FERDIG_GAP")
        assertIkkeForkastedeTilstander(
            2,
            "MOTTATT_SYKMELDING_UFERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP",
            "AVVENTER_UFERDIG_GAP"
        )
    }

    @Test
    fun `overstyring fra saksbehandler fører til tilstandsendring`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendOverstyringTidslinje(listOf(ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)))

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "AVVENTER_HISTORIKK"
        )

        val sisteMelding = testRapid.inspektør.melding(testRapid.inspektør.antall() - 1)
        assertTrue(sisteMelding.hasNonNull("vedtaksperiodeId"))
    }

    @Test
    fun `Send annulleringsevent`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling(0, utbetalingOK = true)

        val fagsystemId = testRapid.inspektør.let { it.melding(it.antall() - 1)["utbetalt"][0]["fagsystemId"] }.asText()
        sendAnnullering(fagsystemId)
        sendUtbetaling(
            0,
            fagsystemId = fagsystemId,
            utbetalingOK = true,
            saksbehandlerEpost = "tbd@nav.no",
            annullert = true
        )

        val meldinger = 0.until(testRapid.inspektør.antall()).map(testRapid.inspektør::melding)
        val utbetalingAnnullert =
            requireNotNull(meldinger.firstOrNull { it["@event_name"]?.asText() == "utbetaling_annullert" })

        assertEquals(fagsystemId, utbetalingAnnullert["fagsystemId"].asText())
        assertEquals("tbd@nav.no", utbetalingAnnullert["saksbehandlerEpost"].asText())
        assertNotNull(utbetalingAnnullert["annullertAvSaksbehandler"].asText())

        assertEquals(19.januar.toString(), utbetalingAnnullert["utbetalingslinjer"][0]["fom"].asText())
        assertEquals(26.januar.toString(), utbetalingAnnullert["utbetalingslinjer"][0]["tom"].asText())
        assertEquals(8586, utbetalingAnnullert["utbetalingslinjer"][0]["beløp"].asInt())
        assertEquals(100, utbetalingAnnullert["utbetalingslinjer"][0]["grad"].asInt())
    }

    @Test
    fun `Utbetalingsevent har automatiskBehandling = true for automatiske behandlinger`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(
            vedtaksperiodeIndeks = 0,
            godkjent = true,
            saksbehandlerIdent = "SYSTEM",
            automatiskBehandling = true
        )
        sendUtbetaling(0, utbetalingOK = true)

        val utbetaltEvent = testRapid.inspektør.let { it.melding(it.antall() - 1) }

        assertEquals("utbetalt", utbetaltEvent["@event_name"].textValue())
        assertTrue(utbetaltEvent["automatiskBehandling"].booleanValue())
        assertEquals("SYSTEM", utbetaltEvent["godkjentAv"].textValue())
    }

    @Test
    fun `Utbetalingsevent har automatiskBehandling = false for manuelle behandlinger`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(
            vedtaksperiodeIndeks = 0,
            godkjent = true,
            saksbehandlerIdent = "O123456",
            automatiskBehandling = false
        )
        sendUtbetaling(0, utbetalingOK = true)

        val utbetaltEvent = testRapid.inspektør.let { it.melding(it.antall() - 1) }

        assertEquals("utbetalt", utbetaltEvent["@event_name"].textValue())
        assertFalse(utbetaltEvent["automatiskBehandling"].booleanValue())
        assertEquals("O123456", utbetaltEvent["godkjentAv"].textValue())
    }

    @Test
    fun `Inntektsmelding med opphør av naturalytelser blir kastet til infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(
            vedtaksperiodeIndeks = 0,
            arbeidsgiverperiode = listOf(Periode(fom = 1.januar, tom = 16.januar)),
            førsteFraværsdag = 1.januar,
            opphørAvNaturalytelser = listOf(
                OpphoerAvNaturalytelse(
                    Naturalytelse.ELEKTRONISKKOMMUNIKASJON,
                    2.januar,
                    BigDecimal(600.0)
                )
            )
        )

        assertForkastedeTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_GAP",
            "TIL_INFOTRYGD"
        )
    }
}


