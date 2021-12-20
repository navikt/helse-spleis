package no.nav.helse.spleis.e2e

import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.*
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class TrengerInntektsmeldingTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender ikke trenger_inntektsmelding i tilfeller hvor vi egentlig har fått inntektsmelding, men har kastet søkander som følge av overlapp og fått kunstig gap`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 21.juli(2021), tom = 4.august(2021), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 21.juli(2021), tom = 4.august(2021), sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 21.juli(2021), tom =  5.august(2021))), førsteFraværsdag = 21.juli(2021))
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVSLUTTET_UTEN_UTBETALING",
            "AVSLUTTET_UTEN_UTBETALING"
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 5.august(2021), tom = 3.september(2021), sykmeldingsgrad = 100))
        sendSøknad(1, listOf(SoknadsperiodeDTO(fom = 5.august(2021), tom = 3.september(2021), sykmeldingsgrad = 100)))
        sendYtelserUtenSykepengehistorikk(1)
        sendVilkårsgrunnlag(1, 1.rangeTo(6).map { YearMonth.of(2021, it) to INNTEKT * 2 })
        sendYtelserUtenSykepengehistorikk(1)
        sendSimulering(1, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(1)
        sendUtbetaling()
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
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

        assertEquals(0, testRapid.inspektør.meldinger("trenger_inntektsmelding").size)
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
    fun `håndterer trenger_inntektsmelding isolert per arbeidsgiver`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)

        val annenArbeidsgiver = "999999999"
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100), orgnummer = annenArbeidsgiver)
        sendSøknad(1, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100)), orgnummer = annenArbeidsgiver)

        assertEquals(1, testRapid
            .inspektør
            .meldinger("trenger_inntektsmelding")
            .filter { it["organisasjonsnummer"].asText() == ORGNUMMER }
            .size
        )
        assertEquals(1, testRapid
            .inspektør
            .meldinger("trenger_ikke_inntektsmelding")
            .filter { it["organisasjonsnummer"].asText() == ORGNUMMER }
            .size
        )
        assertEquals(1, testRapid
            .inspektør
            .meldinger("trenger_inntektsmelding")
            .filter { it["organisasjonsnummer"].asText() == annenArbeidsgiver }
            .size
        )
        assertEquals(0, testRapid
            .inspektør
            .meldinger("trenger_ikke_inntektsmelding")
            .filter { it["organisasjonsnummer"].asText() == annenArbeidsgiver }
            .size
        )
    }
}
