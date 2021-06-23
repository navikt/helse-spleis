package no.nav.helse.spleis.e2e

import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.InntektskildeDTO
import no.nav.syfo.kafka.felles.InntektskildetypeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `tillater søknader med flere arbeidsforhold`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer1")
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer2")
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            orgnummer = "orgnummer1",
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP"
        )

        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            orgnummer = "orgnummer2",
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP"
        )
    }

    @Test
    fun `tillater ikke søknader med !ANDRE_ARBEIDSFORHOLD`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer1")
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer2")
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            orgnummer = "orgnummer1",
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            orgnummer = "orgnummer2",
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.JORDBRUKER_FISKER_REINDRIFTSUTOVER, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD der bruker ikke er sykmeldt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer1")
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer2")
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            orgnummer = "orgnummer1",
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            orgnummer = "orgnummer2",
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, false))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en periode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en periode - inntektsmelding før søknad`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_SØKNAD_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en uferdig periode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 30.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 30.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_UFERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en uferdig periode - inntektsmelding før søknad`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 14.januar, sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 16.januar, tom = 20.januar, sykmeldingsgrad = 100))

        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 14.januar), Periode(16.januar, 19.januar)), førsteFraværsdag = 16.januar)

        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 16.januar, tom = 20.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_UFERDIG_GAP",
            "AVVENTER_SØKNAD_UFERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en forlengelsesperiode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
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

        sendNySøknad(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en uferdig forlengelsesperiode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)

        sendNySøknad(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

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
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
            "TIL_INFOTRYGD"
        )
    }
}
