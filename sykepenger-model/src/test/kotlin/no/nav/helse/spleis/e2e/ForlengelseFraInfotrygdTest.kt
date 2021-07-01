package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.reflect.KClass

internal class ForlengelseFraInfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `setter riktig skjæringstidspunkt`() {
        val historikk1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 29.januar(2018), 18.februar(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 19.februar(2018), 18.mars(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 19.mars(2018), 2.april(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.april(2018), 14.mai(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.mai(2018), 3.juni(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 4.juni(2018), 22.juni(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.mars(2020), 31.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.april(2020), 30.april(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mai(2020), 31.mai(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER, 18.mars(2020), INNTEKT, true),
            Inntektsopplysning(ORGNUMMER, 29.januar(2018), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk1.toTypedArray(), inntektshistorikk = inntektsopplysning1)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        val historikk2 = historikk1 + listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.juli(2020), 31.august(2020), 100.prosent, 1000.daglig),
        )
        val inntektsopplysning2 = inntektsopplysning1 + listOf(
            Inntektsopplysning(ORGNUMMER, 1.juli(2020), INNTEKT, true)
        )

        assertEquals(18.mars(2020), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(ForlengelseFraInfotrygd.JA, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))

        håndterSykmelding(Sykmeldingsperiode(1.september(2020), 30.september(2020), 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.september(2020), 30.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertEquals(18.mars(2020), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(ForlengelseFraInfotrygd.JA, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `forlenger vedtaksperiode som har gått til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar, INNTEKT, true))
        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(3.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(ForlengelseFraInfotrygd.JA, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `forlenger ikke vedtaksperiode som har gått til infotrygd, der utbetaling ikke er gjort`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 25.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar(2018), INNTEKT, true))
        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)  // <-- TIL_INFOTRYGD
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(Periode(29.januar, 13.februar)),
            førsteFraværsdag = 29.januar
        )
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(ForlengelseFraInfotrygd.NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `forlengelsesperiode der refusjon opphører - med søknad for forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(13.mars(2020), 29.mars(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(13.mars(2020), 28.mars(2020))),
            førsteFraværsdag = 13.mars(2020),
            refusjon = Refusjon(31.mars(2020), INNTEKT, emptyList())
        )
        håndterSykmelding(Sykmeldingsperiode(30.mars(2020), 14.april(2020), 100.prosent))
        håndterSøknad(Sykdom(13.mars(2020), 29.mars(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mars(2020), 14.april(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            3.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 13.mars(2020), 29.mars(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    13.mars(2020), INNTEKT, true, 31.mars(2020)
                )
            )
        )
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, TIL_INFOTRYGD)
    }

    @Test
    fun `forlengelsesperiode der refusjon opphører`() {
        håndterSykmelding(Sykmeldingsperiode(13.mars(2020), 29.mars(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(13.mars(2020), 28.mars(2020))),
            førsteFraværsdag = 13.mars(2020),
            refusjon = Refusjon(31.mars(2020), INNTEKT, emptyList())
        )
        håndterSykmelding(Sykmeldingsperiode(30.mars(2020), 14.april(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mars(2020), 14.april(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 13.mars(2020), 29.mars(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    13.mars(2020), INNTEKT, true, 31.mars(2020)
                )
            )
        )
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `avdekker tilstøtende periode i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(
            Inntektsopplysning(
                ORGNUMMER,
                3.januar,
                INNTEKT,
                true
            )
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG, AVVENTER_HISTORIKK, AVVENTER_SIMULERING
        )
        assertEquals(3.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
    }

    @Test
    fun `avdekker tilstøtende periode i Infotrygd uten at vi har Inntektsopplysninger`() {
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig),
            inntektshistorikk = emptyList()
        )
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(18.mars, 31.mars, 100.prosent))
        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, 1000.daglig)
        )  // <-- TIL_INFOTRYGD
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    @Test
    fun `tidligere utbetalinger i spleis som er forkastet blir tatt med som en del av utbetalingshistorikken`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 1.januar)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(1.januar, 31.januar), it.first())
        }

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(
            Sykdom(1.februar, 28.februar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(false, "ANDRE_ARBEIDSFORHOLD")) // <-- for å sende til Infotrygd
        )
        inspektør.låstePerioder.also {
            assertEquals(0, it.size)
        }

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, 1000.daglig))
        håndterUtbetalingsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(1.mars, 31.mars), it.first())
        }

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, TIL_INFOTRYGD)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertEquals(28.desember, inspektør.sisteMaksdato(1.vedtaksperiode))
        assertEquals(inspektør.sisteMaksdato(3.vedtaksperiode), inspektør.sisteMaksdato(1.vedtaksperiode))
    }

    @Test
    fun `lager ikke ny arbeidsgiverperiode når det er tilstøtende historikk`() {
        håndterSykmelding(Sykmeldingsperiode(18.februar(2020), 3.mars(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(18.februar(2020), 3.mars(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(4.mars(2020), 17.mars(2020), 100.prosent))
        håndterSøknad(Sykdom(4.mars(2020), 17.mars(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(18.mars(2020), 15.april(2020), 70.prosent))
        håndterSøknad(Sykdom(18.mars(2020), 15.april(2020), 70.prosent))

        håndterInntektsmelding(listOf(Periode(18.februar(2020), 4.mars(2020))), 18.februar(2020))

        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.februar(2019) til 1.januar(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(110)) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(16.april(2020), 7.mai(2020), 50.prosent))
        håndterSøknad(Sykdom(16.april(2020), 7.mai(2020), 50.prosent))

        håndterUtbetalingshistorikk(
            4.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.mars(2020), 17.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.mars(2020), 15.april(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    5.mars(2020), INNTEKT, true
                )
            )
        )
        håndterUtbetalingsgrunnlag(4.vedtaksperiode)
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode, true)
        håndterUtbetalt(4.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
        assertTilstander(
            4.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        inspektør.utbetalinger.aktive().also { utbetalinger ->
            assertEquals(1, utbetalinger.size)
            UtbetalingstidslinjeInspektør(utbetalinger.first().utbetalingstidslinje()).also {
                assertEquals(0, it.arbeidsgiverperiodeDagTeller)
                assertEquals(16, it.navDagTeller)
            }
        }
    }

    @Test
    fun `setter forlengelse-flagget likt som forrige periode - forlengelse fra infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 3.januar(2018), INNTEKT, true))
        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(24.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(24.februar, 28.februar, 100.prosent))
        håndterUtbetalingsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertEquals(ForlengelseFraInfotrygd.JA, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
        assertEquals(inspektør.forlengelseFraInfotrygd(2.vedtaksperiode), inspektør.forlengelseFraInfotrygd(3.vedtaksperiode))
    }

    @Test
    fun `setter forlengelse-flagget likt som forrige periode - ikke forlengelse fra infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 25.januar, 100.prosent, 1000.daglig)
        val inntekthistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar(2018), INNTEKT, true))
        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekthistorikk) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(Periode(29.januar, 13.februar)),
            førsteFraværsdag = 29.januar
        )
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(29.januar, 23.februar), it.first())
        }
        håndterSykmelding(Sykmeldingsperiode(24.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(24.februar, 28.februar, 100.prosent))
        håndterUtbetalingsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertEquals(ForlengelseFraInfotrygd.NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
        assertEquals(inspektør.forlengelseFraInfotrygd(2.vedtaksperiode), inspektør.forlengelseFraInfotrygd(3.vedtaksperiode))
    }

    @Test
    fun `Forlengelse av søknad uten utbetaling med opphold betalt i Infotrygd`() {
        // Inspirert av et case i P der en overlappende sykmelding ble kastet
        håndterSykmelding(Sykmeldingsperiode(26.mai(2020), 2.juni(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(26.mai(2020), 2.juni(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(26.mai(2020), 2.juni(2020))), førsteFraværsdag = 26.mai(2020))

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100.prosent))

        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 11.juni(2020), 21.juni(2020), 100.prosent, 1000.daglig))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 11.juni(2020), INNTEKT, true))

        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        inspektør.apply {
            assertTrue(etterspurteBehov(2.vedtaksperiode, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering))
            assertTrue(
                utbetalingstidslinjer(1.vedtaksperiode)
                    .filterIsInstance<ArbeidsgiverperiodeDag>().isEmpty()
            )
        }
    }

    @Test
    fun `Forlengelse av søknad med utbetaling med opphold betalt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(10.mai(2020), 2.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(10.mai(2020), 2.juni(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(10.mai(2020), 25.mai(2020))), førsteFraværsdag = 10.mai(2020))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.mai(2019) til 1.april(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.juni(2020), 21.juni(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 10.mai(2020), INNTEKT, true))
        )
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
        inspektør.apply {
            assertTrue(etterspurteBehov(1.vedtaksperiode, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering))
            assertTrue(
                utbetalingstidslinjer(2.vedtaksperiode)
                    .filterIsInstance<ArbeidsgiverperiodeDag>().isEmpty()
            )
            assertTrue(
                utbetalingstidslinjer(1.vedtaksperiode)
                    .filterIsInstance<ArbeidsgiverperiodeDag>().isNotEmpty()
            )
        }
    }

    @Test
    fun `Kort gap mot Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 2.februar)
        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, 1000.daglig))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar(2018), 1000.daglig, true))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertEquals(1, inspektør.utbetalinger.size)
        inspektør.utbetalinger.first().utbetalingstidslinje().also { utbetalingstidslinje ->
            assertAlleDager(utbetalingstidslinje, 1.januar til 16.januar, ArbeidsgiverperiodeDag::class)
            assertAlleDager(utbetalingstidslinje, 17.januar til 1.februar, UkjentDag::class, Fridag::class)
            assertAlleDager(utbetalingstidslinje, 2.februar til 28.februar, NavDag::class, NavHelgDag::class)
        }
    }

    private fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        utbetalingstidslinje.subset(periode).also { tidslinje ->
            assertTrue(tidslinje.all { it::class in dager }) {
                val ulikeDager = tidslinje.filter { it::class !in dager }
                "Forventet at alle dager skal være en av: ${dager.joinToString { it.simpleName ?: "UKJENT" }}.\n" +
                    ulikeDager.joinToString(prefix = "  - ", separator = "\n  - ", postfix = "\n") {
                        "${it.dato} er ${it::class.simpleName}"
                    } + "\nUtbetalingstidslinje:\n" + tidslinje.toString() + "\n"
            }
        }
    }

    @Test
    fun `maksdato blir riktig i ping-pong-perioder`() {
        val historikk1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 20.november(2019), 3.januar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 4.januar(2020), 31.januar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar(2020), 14.februar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.februar(2020), 3.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 4.mars(2020), 20.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 21.mars(2020), 17.april(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.april(2020), 8.mai(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 9.mai(2020), 29.mai(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER, 20.november(2019), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk1.toTypedArray(), inntektshistorikk = inntektsopplysning1)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        val historikk2 = historikk1 + listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.juni(2020), 9.juli(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juli(2020), 31.juli(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.august(2020), 17.august(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning2 = inntektsopplysning1 + listOf(
            Inntektsopplysning(ORGNUMMER, 22.juni(2020), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 2.september(2020), 100.prosent))
        håndterSøknad(Sykdom(18.august(2020), 2.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)


        håndterSykmelding(Sykmeldingsperiode(3.september(2020), 30.september(2020), 100.prosent))
        håndterSøknad(Sykdom(3.september(2020), 30.september(2020), 100.prosent))
        håndterUtbetalingsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(1.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(2.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(3.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(3.vedtaksperiode))
    }

    @Test
    fun `Person uten refusjon til arbeidsgiver blir ikke behandlet i Spleis`() {
        håndterSykmelding(Sykmeldingsperiode(23.oktober(2020), 18.november(2020), 100.prosent))
        håndterSøknad(Sykdom(23.oktober(2020), 18.november(2020), 100.prosent))
        val historikk = arrayOf(
            PersonUtbetalingsperiode(ORGNUMMER, 7.oktober(2019), 1.juli(2020), 100.prosent, 1000.daglig),
            Friperiode(2.juli(2020), 2.september(2020)),
            PersonUtbetalingsperiode(ORGNUMMER, 3.september(2020), 22.oktober(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(ORGNUMMER, 7.oktober(2019), INNTEKT, false)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk, inntektshistorikk = inntektsopplysning)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `ping-pong hvor infotrygd perioden slutter på maksdato skal ikke føre til en automatisk annullering`() {
        val fom1 = 1.juni
        val tom1 = 30.juni
        håndterSykmelding(Sykmeldingsperiode(fom1, tom1, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(fom1, tom1, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.mai, 100.prosent, 1200.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1200.daglig, true, null))
        )
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        val fom2 = 13.desember
        val tom2 = 31.desember
        håndterSykmelding(Sykmeldingsperiode(fom2, tom2, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(fom2, tom2, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.mai, 100.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.juli, 12.desember, 100.prosent, 1200.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1200.daglig, true, null))
        )
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
    }

    @Test
    fun `Ping-pong med ferie mellom Infotrygd-perioder skal ikke beregne nye skjæringstidspunkt etter ferien`() {
        håndterSykmelding(Sykmeldingsperiode(26.juni(2020), 26.juli(2020), 60.prosent))
        håndterSøknad(Sykdom(26.juni(2020), 26.juli(2020), 60.prosent))
        val historikk1 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 100.prosent, 1200.daglig),
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk1, inntektshistorikk = inntektsopplysning1)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(12.oktober(2020), 8.november(2020), 50.prosent))
        håndterSøknad(Sykdom(12.oktober(2020), 8.november(2020), 50.prosent))

        val historikk2 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 60.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 27.juli(2020), 6.september(2020), 60.prosent, 1200.daglig),
            Friperiode(7.september(2020), 8.september(2020)),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 9.september(2020), 13.september(2020), 60.prosent, 1200.daglig),
            Friperiode(14.september(2020), 15.september(2020)),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 16.september(2020), 11.oktober(2020), 60.prosent, 1200.daglig)
        )
        val inntektsopplysning2 = listOf(
            Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true),
            Inntektsopplysning(ORGNUMMER, 27.juli(2020), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk2, inntektshistorikk = inntektsopplysning2)
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(9.november(2020), 6.desember(2020), 60.prosent))
        håndterSøknad(Sykdom(9.november(2020), 6.desember(2020), 60.prosent))
        håndterUtbetalingsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Sender perioden til Infotrygd hvis inntekt mangler ved bygging av utbetalingstidslinje`() {
        håndterSykmelding(Sykmeldingsperiode(26.juni(2020), 26.juli(2020), 60.prosent))
        håndterSøknad(Sykdom(26.juni(2020), 26.juli(2020), 60.prosent))
        val historikk = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.januar(2020), 25.januar(2020), 100.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 100.prosent, 1200.daglig)
        )
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Forlengelse oppdager ferie fra infotrygd`() {
        /* Vi ser at hvis vi oppdager ferie i infotrygd ender vi opp med ukjente dager i utbetalingstidslinja.
           Dette fører til en annullering i oppdraget. */
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent), Ferie(16.februar, 17.februar))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk, besvart = LocalDateTime.now().minusHours(24))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(24.februar, 15.mars, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(24.februar, 15.mars, 100.prosent))
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, historikk, Friperiode(16.februar, 17.februar), inntektshistorikk = inntektshistorikk)
        håndterSimulering(2.vedtaksperiode)
        assertEquals(2, inspektør.arbeidsgiverOppdrag[1].linjerUtenOpphør().size)
    }
}
