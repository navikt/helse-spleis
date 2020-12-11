package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.TilstandType.*
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
            RefusjonTilArbeidsgiver(29.januar(2018), 18.februar(2018), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(19.februar(2018), 18.mars(2018), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(19.mars(2018), 2.april(2018), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(3.april(2018), 14.mai(2018), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(15.mai(2018), 3.juni(2018), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(4.juni(2018), 22.juni(2018), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(18.mars(2020), 31.mars(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(1.april(2020), 30.april(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(1.mai(2020), 31.mai(2020), 1000.daglig,  100.prosent,  ORGNUMMER)
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(18.mars(2020), INNTEKT, ORGNUMMER, true),
            Inntektsopplysning(29.januar(2018), INNTEKT, ORGNUMMER, true)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = historikk1.toTypedArray(), inntektshistorikk = inntektsopplysning1)
        håndterYtelser(1.vedtaksperiode, utbetalinger = historikk1.toTypedArray(), inntektshistorikk = inntektsopplysning1)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        val historikk2 = historikk1 + listOf(
            RefusjonTilArbeidsgiver(1.juli(2020), 31.august(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
        )
        val inntektsopplysning2 = inntektsopplysning1 + listOf(
            Inntektsopplysning(1.juli(2020), INNTEKT, ORGNUMMER, true)
        )

        assertEquals(18.mars(2020), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(ForlengelseFraInfotrygd.JA, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))

        håndterSykmelding(Sykmeldingsperiode(1.september(2020), 30.september(2020), 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.september(2020), 30.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, utbetalinger = historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterYtelser(2.vedtaksperiode, utbetalinger = historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertEquals(18.mars(2020), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(ForlengelseFraInfotrygd.JA, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `forlenger vedtaksperiode som har gått til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000.daglig,  100.prosent,  ORGNUMMER)
        val inntektshistorikk = listOf(Inntektsopplysning(1.januar, INNTEKT, ORGNUMMER, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(2.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(2.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(3.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(ForlengelseFraInfotrygd.JA, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `forlenger ikke vedtaksperiode som har gått til infotrygd, der utbetaling ikke er gjort`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = RefusjonTilArbeidsgiver(3.januar, 25.januar, 1000.daglig,  100.prosent,  ORGNUMMER)
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk)  // <-- TIL_INFOTRYGD
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk)
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(Periode(29.januar, 13.februar)),
            førsteFraværsdag = 29.januar
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode, historikk)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(ForlengelseFraInfotrygd.NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `forlengelsesperiode der refusjon opphører`() {
        håndterSykmelding(Sykmeldingsperiode(13.mars(2020), 29.mars(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(13.mars(2020), 28.mars(2020))),
            førsteFraværsdag = 13.mars(2020),
            refusjon = Triple(31.mars(2020), INNTEKT, emptyList())
        )
        håndterSykmelding(Sykmeldingsperiode(30.mars(2020), 14.april(2020), 100.prosent))
        håndterSøknad(Sykdom(13.mars(2020), 29.mars(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mars(2020), 14.april(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(13.mars(2020), 29.mars(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    13.mars(2020),
                    INNTEKT, ORGNUMMER, true, 31.mars(2020)
                )
            )
        )
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `avdekker tilstøtende periode i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    1.januar,
                    INNTEKT,
                    ORGNUMMER,
                    true
                )
            )
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    1.januar,
                    INNTEKT,
                    ORGNUMMER,
                    true
                )
            )
        )
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertEquals(3.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
    }

    @Test
    fun `avdekker tilstøtende periode i Infotrygd uten at vi har Inntektsopplysninger`() {
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = emptyList()
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = emptyList()
        )
        assertForkastetPeriodeTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(18.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, RefusjonTilArbeidsgiver(
                1.januar, 31.januar, 1000.daglig,
                100.prosent,
                ORGNUMMER
            )
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
            AVVENTER_GAP
        )
    }

    @Test
    fun `tidligere utbetalinger i spleis som er forkastet blir tatt med som en del av utbetalingshistorikken`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 1.januar)
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
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Permisjon(10.februar, 20.februar))    // <-- TIL_INFOTRYGD
        inspektør.låstePerioder.also {
            assertEquals(0, it.size)
        }

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, RefusjonTilArbeidsgiver(1.februar, 28.februar, 1000.daglig, 100.prosent, ORGNUMMER))
        håndterYtelser(3.vedtaksperiode, RefusjonTilArbeidsgiver(1.februar, 28.februar, 1000.daglig, 100.prosent, ORGNUMMER))
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
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
            AVVENTER_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertEquals(28.desember, inspektør.maksdato(1.vedtaksperiode))
        assertEquals(inspektør.maksdato(3.vedtaksperiode), inspektør.maksdato(1.vedtaksperiode))
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

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(5)) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(16.april(2020), 7.mai(2020), 50.prosent))
        håndterSøknad(Sykdom(16.april(2020), 7.mai(2020), 50.prosent))

        håndterUtbetalingshistorikk(
            4.vedtaksperiode,
            RefusjonTilArbeidsgiver(5.mars(2020), 17.mars(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(18.mars(2020), 15.april(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    5.mars(2020),
                    INNTEKT, ORGNUMMER, true
                )
            )
        )
        håndterYtelser(
            4.vedtaksperiode,
            RefusjonTilArbeidsgiver(5.mars(2020), 17.mars(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(18.mars(2020), 15.april(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    5.mars(2020),
                    INNTEKT, ORGNUMMER, true
                )
            )
        )
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode, true)
        håndterUtbetalt(4.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
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
            AVVENTER_GAP,
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
        val historikk = RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000.daglig,  100.prosent,  ORGNUMMER)
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk)
        håndterYtelser(2.vedtaksperiode, historikk)
        håndterYtelser(2.vedtaksperiode, historikk)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(24.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(24.februar, 28.februar, 100.prosent))
        håndterYtelser(3.vedtaksperiode, historikk)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
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
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertEquals(ForlengelseFraInfotrygd.JA, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
        assertEquals(inspektør.forlengelseFraInfotrygd(2.vedtaksperiode), inspektør.forlengelseFraInfotrygd(3.vedtaksperiode))
    }

    @Test
    fun `setter forlengelse-flagget likt som forrige periode - ikke forlengelse fra infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = RefusjonTilArbeidsgiver(3.januar, 25.januar, 1000.daglig,  100.prosent,  ORGNUMMER)
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk)
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(Periode(29.januar, 13.februar)),
            førsteFraværsdag = 29.januar
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode, historikk)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(29.januar, 23.februar), it.first())
        }
        håndterSykmelding(Sykmeldingsperiode(24.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(24.februar, 28.februar, 100.prosent))
        håndterYtelser(3.vedtaksperiode, historikk)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(11.juni(2020), 21.juni(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(Inntektsopplysning(26.mai(2020), INNTEKT, ORGNUMMER, true))
        )
        håndterYtelser(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(11.juni(2020), 21.juni(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(Inntektsopplysning(26.mai(2020), INNTEKT, ORGNUMMER, true))
        )
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
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.juni(2020), 21.juni(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(Inntektsopplysning(10.mai(2020), INNTEKT, ORGNUMMER, true))
        )
        håndterYtelser(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(3.juni(2020), 21.juni(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            inntektshistorikk = listOf(Inntektsopplysning(10.mai(2020), INNTEKT, ORGNUMMER, true))
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
            AVVENTER_GAP,
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
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(17.januar, 31.januar, 1000.daglig,  100.prosent,  ORGNUMMER))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertEquals(1, inspektør.utbetalinger.size)
        inspektør.utbetalinger.first().utbetalingstidslinje().also { utbetalingstidslinje ->
            assertAlleDager(utbetalingstidslinje, 1.januar til 16.januar, ArbeidsgiverperiodeDag::class)
            assertAlleDager(utbetalingstidslinje, 17.januar til 31.januar, UkjentDag::class, Fridag::class)
            assertTrue(utbetalingstidslinje[1.februar] is Arbeidsdag)
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
            RefusjonTilArbeidsgiver(20.november(2019), 3.januar(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(4.januar(2020), 31.januar(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(1.februar(2020), 14.februar(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(15.februar(2020), 3.mars(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(4.mars(2020), 20.mars(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(21.mars(2020), 17.april(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(18.april(2020), 8.mai(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(9.mai(2020), 29.mai(2020), 1000.daglig,  100.prosent,  ORGNUMMER)
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(20.november(2019), INNTEKT, ORGNUMMER, true)
        )

        håndterSykmelding(Sykmeldingsperiode(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = historikk1.toTypedArray(), inntektshistorikk = inntektsopplysning1)
        håndterYtelser(1.vedtaksperiode, utbetalinger = historikk1.toTypedArray(), inntektshistorikk = inntektsopplysning1)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        val historikk2 = historikk1 + listOf(
            RefusjonTilArbeidsgiver(22.juni(2020), 9.juli(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(10.juli(2020), 31.juli(2020), 1000.daglig,  100.prosent,  ORGNUMMER),
            RefusjonTilArbeidsgiver(1.august(2020), 17.august(2020), 1000.daglig,  100.prosent,  ORGNUMMER)
        )
        val inntektsopplysning2 = inntektsopplysning1 + listOf(
            Inntektsopplysning(22.juni(2020), INNTEKT, ORGNUMMER, true)
        )

        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 2.september(2020), 100.prosent))
        håndterSøknad(Sykdom(18.august(2020), 2.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, utbetalinger = historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterYtelser(2.vedtaksperiode, utbetalinger = historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)


        håndterSykmelding(Sykmeldingsperiode(3.september(2020), 30.september(2020), 100.prosent))
        håndterSøknad(Sykdom(3.september(2020), 30.september(2020), 100.prosent))
        håndterYtelser(3.vedtaksperiode, utbetalinger = historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterYtelser(3.vedtaksperiode, utbetalinger = historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(30.oktober(2020), inspektør.maksdato(1.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.maksdato(2.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.maksdato(3.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(3.vedtaksperiode))
    }

    @Test
    fun `Person uten refusjon til arbeidsgiver blir ikke behandlet i Spleis`() {
        håndterSykmelding(Sykmeldingsperiode(23.oktober(2020), 18.november(2020), 100.prosent))
        håndterSøknad(Sykdom(23.oktober(2020), 18.november(2020), 100.prosent))
        val historikk = arrayOf(
            Utbetalingshistorikk.Infotrygdperiode.Utbetaling(7.oktober(2019), 1.juli(2020), 1000.daglig, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Ferie(2.juli(2020), 2.september(2020)),
            Utbetalingshistorikk.Infotrygdperiode.Utbetaling(3.september(2020), 22.oktober(2020), 1000.daglig, 100.prosent, ORGNUMMER)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(7.oktober(2019), INNTEKT, ORGNUMMER, false)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = historikk, inntektshistorikk = inntektsopplysning)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
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
            RefusjonTilArbeidsgiver(1.januar, 31.mai, 1200.daglig, 100.prosent, ORGNUMMER)
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.mai, 1200.daglig, 100.prosent, ORGNUMMER),
            inntektshistorikk = listOf(Inntektsopplysning(1.januar, 1200.daglig, ORGNUMMER, true, null))
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)


        val fom2 = 13.desember
        val tom2 = 31.desember
        håndterSykmelding(Sykmeldingsperiode(fom2, tom2, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(fom2, tom2, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.mai, 1200.daglig, 100.prosent, ORGNUMMER),
            RefusjonTilArbeidsgiver(1.juli, 12.desember, 1200.daglig, 100.prosent, ORGNUMMER)
        )
        håndterYtelser(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.mai, 1200.daglig, 100.prosent, ORGNUMMER),
            RefusjonTilArbeidsgiver(1.juli, 12.desember, 1200.daglig, 100.prosent, ORGNUMMER),
            inntektshistorikk = listOf(Inntektsopplysning(1.januar, 1200.daglig, ORGNUMMER, true, null))
        )
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET)
        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
    }
}
