package no.nav.helse.spleis.e2e

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Toggle
import no.nav.helse.Toggle.Companion.disable
import no.nav.helse.Toggle.Companion.enable
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.TestHendelseMediator
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.*
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class KunEnArbeidsgiverMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `ingen historie med Søknad først`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
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
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
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
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendUtbetalingsgodkjenning(0, true)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
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
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
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
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "AVVENTER_HISTORIKK"
        )
    }

    @Test
    fun `overstyring av tidslinje fra saksbehandler fører til tilstandsendring`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
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
    fun `overstyring av inntekt fra saksbehandler fører til tilstandsendring`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar, beregnetInntekt = 33000.0)
        sendOverstyringInntekt(33000.0, 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "FORKASTET")
        assertUtbetalingTilstander(1, "IKKE_UTBETALT", "GODKJENT", "SENDT")
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "AVVENTER_VILKÅRSPRØVING",
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
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.TEKNISK_FEIL)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING"
        )
    }

    @Test
    fun `replayer inntektsmeldinger hvis er i gap og venter på inntektsmelding`() {
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100)))

        assertTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP", "AVVENTER_HISTORIKK")
    }

    @Test
    fun `sender ikke trenger_inntektsmelding i tilfeller hvor vi egentlig har fått inntektsmelding, men har kastet søkander som følge av overlapp og fått kunstig gap`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 21.juli(2021), tom = 4.august(2021), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 21.juli(2021), tom = 4.august(2021), sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 21.juli(2021), tom =  5.august(2021))), førsteFraværsdag = 21.juli(2021))
        sendYtelser(0)
        sendVilkårsgrunnlag(0, 1.rangeTo(6).map { YearMonth.of(2021, it) to INNTEKT * 2 })
        sendYtelserUtenSykepengehistorikk(0)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVSLUTTET_UTEN_UTBETALING"
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 5.august(2021), tom = 3.september(2021), sykmeldingsgrad = 100))
        sendSøknad(1, listOf(SoknadsperiodeDTO(fom = 5.august(2021), tom = 3.september(2021), sykmeldingsgrad = 100)))
        sendYtelserUtenSykepengehistorikk(1)
        sendSimulering(1, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(1)
        sendUtbetaling()
        assertUtbetalingTilstander(1, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 20.juli(2021), tom = 13.august(2021), sykmeldingsgrad = 100))
        sendSøknadUtenVedtaksperiode(listOf(SoknadsperiodeDTO(fom = 20.juli(2021), tom = 13.august(2021), sykmeldingsgrad = 100)))

        sendNySøknad(SoknadsperiodeDTO(fom = 14.august(2021), tom = 6.september(2021), sykmeldingsgrad = 100))
        sendSøknadUtenVedtaksperiode(listOf(SoknadsperiodeDTO(fom = 14.august(2021), tom = 6.september(2021), sykmeldingsgrad = 100)))

        sendNySøknad(SoknadsperiodeDTO(fom = 7.september(2021), tom = 30.september(2021), sykmeldingsgrad = 100))
        sendSøknad(2, listOf(SoknadsperiodeDTO(fom = 7.september(2021), tom = 30.september(2021), sykmeldingsgrad = 100)))
        assertTilstander(
            2,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
        )

        assertEquals(1, testRapid.inspektør.meldinger("trenger_inntektsmelding").size)
        assertEquals(21.juli(2021), testRapid.inspektør.siste("trenger_inntektsmelding")["fom"].asLocalDate())
    }

    @Test
    fun `sender ikke trenger_inntektsmelding hvor inntektsmelding har førsteFraværsdag i perioden, men arbeidsgiverperioden er før`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 17.januar, tom = 16.februar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 17.januar, tom = 16.februar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 17.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling()
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknadUtenVedtaksperiode(listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100)))

        sendNySøknad(SoknadsperiodeDTO(fom = 28.februar, tom = 16.mars, sykmeldingsgrad = 100))
        sendSøknad(1, listOf(SoknadsperiodeDTO(fom = 28.februar, tom = 16.mars, sykmeldingsgrad = 100)))
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP"
        )

        assertEquals(1, testRapid.inspektør.meldinger("trenger_inntektsmelding").size)
        assertEquals(17.januar, testRapid.inspektør.siste("trenger_inntektsmelding")["fom"].asLocalDate())
    }


    @Test
    fun `Sender med vedtakFattetTidspunkt i vedtak_fattet`() {
        val vedtakFattetTidspunkt = LocalDateTime.now().plusMinutes(1)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true, godkjenttidspunkt = vedtakFattetTidspunkt)
        sendUtbetaling()
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")

        assertEquals(1, testRapid.inspektør.meldinger("vedtak_fattet").size)
        assertEquals(vedtakFattetTidspunkt, testRapid.inspektør.siste("vedtak_fattet")["vedtakFattetTidspunkt"].asLocalDateTime())
    }

    @Test
    fun `Sender med vedtakFattetTidspunkt i vedtak_fattet for perioder som avsluttes uten utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 16.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 16.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVSLUTTET_UTEN_UTBETALING"
        )
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "GODKJENT_UTEN_UTBETALING")

        assertEquals(1, testRapid.inspektør.meldinger("vedtak_fattet").size)
        // Sjekker på localdate fordi modellen slenger på LocalDateTime.now() ved automatisk godkjenning, og det er vanskelig å teste på.
        assertEquals(LocalDate.now(), testRapid.inspektør.siste("vedtak_fattet")["vedtakFattetTidspunkt"].asLocalDateTime().toLocalDate())
    }

    @Test
    fun `trenger_inntektsmelding håndterer korrigerende sykmelding som forkorter perioden`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendNySøknad(SoknadsperiodeDTO(fom = 2.februar, tom = 12.februar, sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 2.februar, tom = 8.februar, sykmeldingsgrad = 100))
        sendSøknad(1, listOf(SoknadsperiodeDTO(fom = 2.februar, tom = 8.februar, sykmeldingsgrad = 100)))

        assertEquals(2, testRapid.inspektør.meldinger("trenger_inntektsmelding").size)
    }

    @Test
    fun `Behandler ikke melding hvis den allerede er behandlet`() {
        val hendelseRepository: HendelseRepository = mockk(relaxed = true)
        every { hendelseRepository.erBehandlet(any()) } returnsMany(listOf(false, true))

        MessageMediator(
            rapidsConnection = testRapid,
            hendelseRepository = hendelseRepository,
            hendelseMediator = TestHendelseMediator()
        )

        val meldingId = UUID.randomUUID()
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 25.januar, sykmeldingsgrad = 100), meldingId = meldingId.toString())
        verify(exactly = 1) { hendelseRepository.markerSomBehandlet(meldingId) }
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 26.januar, sykmeldingsgrad = 100), meldingId = meldingId.toString())
        verify(exactly = 1) { hendelseRepository.markerSomBehandlet(meldingId) }
        verify(exactly = 2) { hendelseRepository.erBehandlet(any()) }
    }

    @Test
    fun `Behandler melding hvis den tidligere har prøvd å behandle melding, men kræsjet`() {
        val hendelseRepository: HendelseRepository = mockk(relaxed = true)
        every { hendelseRepository.erBehandlet(any()) } returnsMany(listOf(false, false, true))

        MessageMediator(
            rapidsConnection = testRapid,
            hendelseRepository = hendelseRepository,
            hendelseMediator = TestHendelseMediator()
        )

        val meldingId = UUID.randomUUID()
        sendNySøknad(SoknadsperiodeDTO(fom = 25.januar, tom = 1.januar, sykmeldingsgrad = 100), meldingId = meldingId.toString())
        verify(exactly = 0) { hendelseRepository.markerSomBehandlet(meldingId) }
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 25.januar, sykmeldingsgrad = 100), meldingId = meldingId.toString())
        verify(exactly = 1) { hendelseRepository.markerSomBehandlet(meldingId) }
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 25.januar, sykmeldingsgrad = 100), meldingId = meldingId.toString())
        verify(exactly = 1) { hendelseRepository.markerSomBehandlet(meldingId) }
        verify(exactly = 3) { hendelseRepository.erBehandlet(any()) }
    }

    @Test
    fun `InntektsmeldingReplay blir ikke stoppet av duplikatsjekk`() {
        val meldingId = UUID.randomUUID()

        val hendelseRepository: HendelseRepository = mockk(relaxed = true)
        MessageMediator(
            rapidsConnection = testRapid,
            hendelseRepository = hendelseRepository,
            hendelseMediator = TestHendelseMediator()
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 26.januar, sykmeldingsgrad = 100)))

        verify(exactly = 0) { hendelseRepository.markerSomBehandlet(meldingId) }
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar, meldingId = meldingId.toString())
        verify(exactly = 1) { hendelseRepository.markerSomBehandlet(meldingId) }
        sendInntektsmeldingReplay(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar, meldingId = meldingId.toString())
        verify(exactly = 2) { hendelseRepository.markerSomBehandlet(meldingId) }
    }

    @Disabled("https://trello.com/c/Ob6kSelp")
    @Test
    fun `spleis sender korrekt grad (avrundet) ut`() {
        sendNySøknad(
            SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 30)
        )
        sendSøknad(
            0, listOf(
                SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 30, faktiskGrad = 80)
            )
        )
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelserUtenSykepengehistorikk(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling()
        assertEquals(20.0, testRapid.inspektør.siste("utbetalt").path("utbetalt").first().path("utbetalingslinjer").first().path("grad").asDouble())
    }

    @Test
    fun `delvis refusjon`() {
        listOf(Toggle.DelvisRefusjon, Toggle.LageBrukerutbetaling).enable {
            sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
            sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
            sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar, opphørsdatoForRefusjon = 20.januar)
            sendYtelser(0)
            sendVilkårsgrunnlag(0)
            sendYtelserUtenSykepengehistorikk(0)
            sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
            sendUtbetalingsgodkjenning(0)
            sendUtbetaling()
            assertUtbetalingTilstander(0, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")
            assertEquals(2, testRapid.inspektør.alleEtterspurteBehov(Utbetaling).size)
            assertTilstander(
                0,
                "MOTTATT_SYKMELDING_FERDIG_GAP",
                "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
                "AVVENTER_HISTORIKK",
                "AVVENTER_VILKÅRSPRØVING",
                "AVVENTER_HISTORIKK",
                "AVVENTER_SIMULERING",
                "AVVENTER_GODKJENNING",
                "TIL_UTBETALING",
                "AVSLUTTET"
            )
        }
    }

    @Test
    fun `delvis refusjon og brukerutbetaling - ikke lov`() {
        listOf(Toggle.DelvisRefusjon, Toggle.LageBrukerutbetaling).disable {
            sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
            sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
            sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar, opphørsdatoForRefusjon = 20.januar)
            sendYtelser(0)
            sendVilkårsgrunnlag(0)
            sendYtelserUtenSykepengehistorikk(0)
            assertUtbetalingTilstander(0, "IKKE_UTBETALT", "FORKASTET")
            assertTilstander(
                0,
                "MOTTATT_SYKMELDING_FERDIG_GAP",
                "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
                "AVVENTER_HISTORIKK",
                "AVVENTER_VILKÅRSPRØVING",
                "AVVENTER_HISTORIKK",
                "TIL_INFOTRYGD"
            )
        }
    }

    @Test
    fun `delvis refusjon - ikke lov`() {
        Toggle.DelvisRefusjon.disable {
            Toggle.LageBrukerutbetaling.enable {
                sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
                sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
                sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar, opphørsdatoForRefusjon = 20.januar)
                sendYtelser(0)
                sendVilkårsgrunnlag(0)
                sendYtelserUtenSykepengehistorikk(0)
                assertUtbetalingTilstander(0, "IKKE_UTBETALT", "FORKASTET")
                assertTilstander(
                    0,
                    "MOTTATT_SYKMELDING_FERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
                    "AVVENTER_HISTORIKK",
                    "AVVENTER_VILKÅRSPRØVING",
                    "AVVENTER_HISTORIKK",
                    "TIL_INFOTRYGD"
                )
            }
        }
    }
}
