package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class OutOfOrderE2ETest : AbstractEndToEndTest() {
    @Test
    fun `korte arbeidsgiversøknader - forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(11.januar, 12.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(11.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 10.januar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `korte arbeidsgiversøknader - gap`() {
        håndterSykmelding(Sykmeldingsperiode(11.januar, 12.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(11.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 9.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 9.januar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `sykmelding før korte arbeidsgiversøknader med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 4.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.mars, 9.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(15.mars, 16.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                3.mars til 4.mars,
                8.mars til 9.mars,
                15.mars til 26.mars
            ), 15.mars
        )

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `sykmelding foran periode AvventerSøknadUferdigForlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 4.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(6.mars, 8.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.mars, 10.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.mars, 22.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(23.mars, 31.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                3.mars til 4.mars,
                6.mars til 8.mars,
                9.mars til 10.mars,
                13.mars til 22.mars
            ), 13.mars
        )

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_SØKNAD_UFERDIG_FORLENGELSE)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP)
        assertTilstander(5.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(6.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `korte arbeidsgiversøknader med inntektsmelding - gap`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(15.januar, 16.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 9.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                3.januar til 4.januar,
                8.januar til 9.januar,
                15.januar til 26.januar
            ), 15.januar
        )

        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 4.januar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP
        )
    }

    @Test
    fun `korte arbeidsgiversøknader med inntektsmelding - gap 2`() {
        håndterSykmelding(Sykmeldingsperiode(8.januar, 9.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(15.januar, 16.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                3.januar til 4.januar,
                8.januar til 9.januar,
                15.januar til 26.januar
            ), 15.januar
        )

        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 4.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `korte arbeidsgiversøknader før søknad`() {
        håndterSykmelding(Sykmeldingsperiode(8.januar, 9.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(15.januar, 16.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))

        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(15.januar, 16.januar, 100.prosent))
        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(3.januar, 4.januar, 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(
                3.januar til 4.januar,
                8.januar til 9.januar,
                15.januar til 26.januar
            ), 15.januar
        )
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(8.januar, 9.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP,
            AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP,
            AVVENTER_HISTORIKK
        )
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `periode foran uten utbetaling med inntektsmelding uferdig gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(12.mars, 19.mars, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 17.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(12.mars, 19.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 17.januar), 1.januar)
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(
                12.mars til 19.mars,
                21.mars til 28.mars
            ),
            21.mars
        )
        håndterSykmelding(Sykmeldingsperiode(12.februar, 19.februar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP
        )
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `periode foran uten utbetaling med inntektsmelding uferdig forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.mars, 11.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(12.mars, 19.mars, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 17.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(11.mars, 11.mars, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(12.mars, 19.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 17.januar), 1.januar)
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(
                11.mars til 11.mars,
                12.mars til 19.mars,
                21.mars til 27.mars
            ),
            21.mars
        )
        håndterSykmelding(Sykmeldingsperiode(12.februar, 19.februar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `Håndterer ny sykmelding som ligger tidligere i tid`() {
        håndterSykmelding(Sykmeldingsperiode(23.mars(2020), 29.mars(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(30.mars(2020), 2.april(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.april(2020), 20.april(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.mars(2020), 22.mars(2020), 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `Håndterer ny sykmelding som ligger tidligere i tid med søknader`() {
        håndterSykmelding(Sykmeldingsperiode(23.mars(2020), 29.mars(2020), 100.prosent))
        håndterSøknad(Sykdom(23.mars(2020), 29.mars(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(30.mars(2020), 2.april(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mars(2020), 2.april(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.april(2020), 20.april(2020), 100.prosent))
        håndterSøknad(Sykdom(10.april(2020), 20.april(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.mars(2020), 22.mars(2020), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen`() {
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen med søknad`() {
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen uferdig`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen uferdig med søknad`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen uferdig med søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.desember(2017) til 16.desember(2017)), førsteFraværsdag = 11.januar)
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen ferdig med søknad `() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen ferdig med søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_UFERDIG_GAP
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen i avventer vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_UFERDIG_GAP
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen i avventer simulering`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_UFERDIG_GAP
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen i avventer godkjenning`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_UFERDIG_GAP
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding før en annen utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `ny sykmelding etter en annen utbetalt, før en annen med sykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100.prosent))

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP
        )
    }

    @Test
    fun `ny sykmelding etter en annen utbetalt, før en annen med søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100.prosent))

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP
        )
    }

    @Test
    fun `ny sykmelding etter en annen utbetalt, før en annen med søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(19.februar til 27.februar))

        håndterSykmelding(Sykmeldingsperiode(19.februar, 28.februar, 100.prosent))

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding med søknad etter en annen utbetalt, før en annen med søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        val inntektsmelding = håndterInntektsmelding(listOf(19.februar til 27.februar))

        håndterSykmelding(Sykmeldingsperiode(19.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(19.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmelding, 3.vedtaksperiode)

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `ny sykmelding etter en annen utbetalt, før en annen med søknad og inntektsmelding som ikke er forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        håndterInntektsmelding(listOf(1.april til 16.april))

        håndterSykmelding(Sykmeldingsperiode(19.februar, 28.februar, 100.prosent))

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_UFERDIG_GAP
        )
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny sykmelding etter en annen utbetalt, før en annen til utbetaling som ikke er forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        håndterInntektsmelding(listOf(1.april til 16.april))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2017) til 1.mars inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode) // TODO: denne må jobbes med fordi her må evt. utbetalingen annulleres!

        håndterSykmelding(Sykmeldingsperiode(19.februar, 28.februar, 100.prosent))

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `ny sykmelding før en ferdig forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(10.februar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.februar, 25.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(10.februar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 9.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }
    @Test
    fun `ny sykmelding før en ferdig forlengelse med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(10.februar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.februar, 25.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(10.februar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(16.februar, 25.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 9.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }
}
