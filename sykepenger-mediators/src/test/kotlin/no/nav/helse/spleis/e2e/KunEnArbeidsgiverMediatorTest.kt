package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Naturalytelse
import no.nav.inntektsmeldingkontrakt.OpphoerAvNaturalytelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.FravarDTO
import no.nav.syfo.kafka.felles.FravarstypeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class KunEnArbeidsgiverMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `ingen historie med Søknad først`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )
    }

    @Test
    fun `bare ferie`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            0,
            listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            fravær = listOf(FravarDTO(19.januar, 26.januar, FravarstypeDTO.FERIE))
        )
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVSLUTTET_UTEN_UTBETALING"
        )
    }

    @Test
    fun `bare permisjon`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            0,
            listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            fravær = listOf(FravarDTO(19.januar, 26.januar, FravarstypeDTO.PERMISJON))
        )
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendUtbetalingsgodkjenning(0, true)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_GODKJENNING",
            "AVSLUTTET_UTEN_UTBETALING"
        )
    }

    @Test
    fun `ikke godkjent utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, false)
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "IKKE_GODKJENT")
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `perioder påvirket av annullering-event blir forkastet men forblir i Avsluttet`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        val fagsystemId = testRapid.inspektør.let { it.siste("utbetalt")["utbetalt"][0]["fagsystemId"] }.asText()
        sendAnnullering(fagsystemId)
        sendUtbetaling()
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")
        assertUtbetalingTilstander(1, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "ANNULLERT")
        val annulleringsmelding = testRapid.inspektør.siste("utbetaling_annullert")

        assertEquals(UNG_PERSON_FNR_2018, annulleringsmelding.path("fødselsnummer").asText())
        assertEquals(AKTØRID, annulleringsmelding.path("aktørId").asText())
        assertEquals(ORGNUMMER, annulleringsmelding.path("organisasjonsnummer").asText())
        assertForkastedeTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )
    }

    @Test
    fun `kan ikke utbetale på overstyrt utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendOverstyringTidslinje(listOf(ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)))
        sendUtbetalingsgodkjenning(0, true)
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "FORKASTET")
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "AVVENTER_HISTORIKK"
        )
    }

    @Test
    fun `overstyring fra saksbehandler fører til tilstandsendring`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendOverstyringTidslinje(listOf(ManuellOverskrivingDag(25.januar, Dagtype.Permisjonsdag), ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)))
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "FORKASTET")
        assertUtbetalingTilstander(1, "IKKE_UTBETALT", "GODKJENT", "SENDT")
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING"
        )
    }

    @Test
    fun `Send annulleringsevent`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling(utbetalingOK = true)

        val fagsystemId = testRapid.inspektør.let { it.siste("utbetalt")["utbetalt"][0]["fagsystemId"] }.asText()
        sendAnnullering(fagsystemId)
        sendUtbetaling(
            utbetalingOK = true
        )

        val utbetalingAnnullert = testRapid.inspektør.siste("utbetaling_annullert")

        assertEquals(fagsystemId, utbetalingAnnullert["fagsystemId"].asText())
        assertEquals("siri.saksbehandler@nav.no", utbetalingAnnullert["saksbehandlerEpost"].asText())
        assertNotNull(utbetalingAnnullert["annullertAvSaksbehandler"].asText())

        assertEquals(19.januar.toString(), utbetalingAnnullert["fom"].asText())
        assertEquals(26.januar.toString(), utbetalingAnnullert["tom"].asText())
        assertEquals(19.januar.toString(), utbetalingAnnullert["utbetalingslinjer"][0]["fom"].asText())
        assertEquals(26.januar.toString(), utbetalingAnnullert["utbetalingslinjer"][0]["tom"].asText())
        assertEquals(0, utbetalingAnnullert["utbetalingslinjer"][0]["beløp"].asInt())
        assertEquals(0, utbetalingAnnullert["utbetalingslinjer"][0]["grad"].asInt())
    }

    @Test
    fun `Utbetalingsevent har automatiskBehandling = true for automatiske behandlinger`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(
            vedtaksperiodeIndeks = 0,
            godkjent = true,
            saksbehandlerIdent = "SYSTEM",
            automatiskBehandling = true
        )
        sendUtbetaling(utbetalingOK = true)

        val utbetaltEvent = testRapid.inspektør.siste("utbetalt")
        assertTrue(utbetaltEvent["automatiskBehandling"].booleanValue())
        assertEquals("SYSTEM", utbetaltEvent["godkjentAv"].textValue())
    }

    @Test
    fun `Utbetalingsevent har automatiskBehandling = false for manuelle behandlinger`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(
            vedtaksperiodeIndeks = 0,
            godkjent = true,
            saksbehandlerIdent = "O123456",
            automatiskBehandling = false
        )
        sendUtbetaling(utbetalingOK = true)

        val utbetaltEvent = testRapid.inspektør.siste("utbetalt")
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
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `ignorerer teknisk feil ved simuleringer`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.TEKNISK_FEIL)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING"
        )
    }

    @Test
    fun `replayer inntektsmeldinger hvis er i gap og venter på inntektsmelding og forrige periode er sendt til IT`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 15.januar, sykmeldingsgrad = 100))
        sendSøknadArbeidsgiver(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 15.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar, opphørsdatoForRefusjon = 1.januar)

        sendNySøknad(SoknadsperiodeDTO(fom = 16.januar, tom = 25.januar, sykmeldingsgrad = 100))
        sendSøknad(1, listOf(SoknadsperiodeDTO(fom = 16.januar, tom = 25.januar, sykmeldingsgrad = 100)))
        sendUtbetalingshistorikk(1)
        sendNyPåminnelse(1, TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        assertForkastedeTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVSLUTTET_UTEN_UTBETALING")
        assertForkastedeTilstander(1, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP", "TIL_INFOTRYGD")
    }

    @Disabled("https://trello.com/c/Ob6kSelp")
    @Test
    fun `spleis sender korrekt grad (avrundet) ut`() {
        sendNySøknad(
            SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 30)
        )
        sendSøknad(0, listOf(
            SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 30, faktiskGrad = 80)
        ))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelserUtenSykepengehistorikk(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling()
        assertEquals(20.0, testRapid.inspektør.siste("utbetalt").path("utbetalt").first().path("utbetalingslinjer").first().path("grad").asDouble())
    }
}
