package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver
import no.nav.helse.person.ForlengelseFraInfotrygd.JA
import no.nav.helse.person.ForlengelseFraInfotrygd.NEI
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class DeleGrunnlagsdataTest : AbstractEndToEndTest() {

    @Test
    fun `vilkårsgrunnlag deles med påfølgende tilstøtende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(5.april, 30.april, 100.prosent))
        val inntektsmelding1Id = håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        val inntektsmelding2Id = håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 5.april)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)
        håndterVilkårsgrunnlag(4.vedtaksperiode, INNTEKT)
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode)
        håndterUtbetalt(4.vedtaksperiode)

        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(3.vedtaksperiode))
        assertNotEquals(inspektør.vilkårsgrunnlag(3.vedtaksperiode), inspektør.vilkårsgrunnlag(4.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(1.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(2.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(3.vedtaksperiode))
        assertTrue(inntektsmelding2Id in inspektør.hendelseIder(4.vedtaksperiode))
    }

    @Test
    fun `vilkårsgrunnlag deles med foregående`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(5.april, 30.april, 100.prosent))

        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(3.vedtaksperiode))
        assertNull(inspektør.vilkårsgrunnlag(4.vedtaksperiode))
    }

    @Test
    fun `vilkårsgrunnlag hentes ikke når perioden er overgang fra IT`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000.daglig,  100.prosent,  ORGNUMMER)
        )
        håndterYtelser(
            1.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000.daglig, 100.prosent, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(
                    1.januar(2018),
                    INNTEKT,
                    ORGNUMMER,
                    true
                )
            )
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000.daglig,  100.prosent,  ORGNUMMER))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `ingen vilkårsgrunnlag når perioden har opphav i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(25.februar, 28.februar))
        håndterUtbetalingshistorikk(1.vedtaksperiode, RefusjonTilArbeidsgiver(17.januar, 31.januar, 15000.daglig,  100.prosent,  ORGNUMMER))
        håndterYtelser(
            1.vedtaksperiode, RefusjonTilArbeidsgiver(17.januar, 31.januar, 15000.daglig, 100.prosent, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(
                    17.januar(2018),
                    INNTEKT,
                    ORGNUMMER,
                    true
                )
            )
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.mars)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, RefusjonTilArbeidsgiver(17.januar, 31.januar, 15000.daglig,  100.prosent,  ORGNUMMER))
        assertTilstander(1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertNotNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `inntektsmelding bryter ikke opp forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(18.januar, 1.februar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(18.januar, 1.februar)), førsteFraværsdag = 4.mars)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
        assertEquals(18.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(18.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `setter ikke inntektsmeldingId flere ganger`() {
        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(20.februar, 28.februar, 100.prosent))

        val sykmeldingId = håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        val søknadId = håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(20.februar, 8.mars)), 20.februar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, AVVENTER_UFERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertEquals(3, inspektør.hendelseIder(2.vedtaksperiode).size)
        assertTrue(inspektør.hendelseIder(2.vedtaksperiode).containsAll(listOf(sykmeldingId, søknadId, inntektsmeldingId)))
    }

    @Test
    fun `vilkårsgrunnlag tilbakestilles når vi ikke er en forlengelse likevel`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent),
            Arbeid(18.januar, 21.januar)
        )
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode) // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertNotNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertNotEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `får egen vilkårsgrunnlag selv når inntektsmelding overlapper, men er gap mellom periodene`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 4.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 15.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 4.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 15.januar, 100.prosent))

        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 15.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        assertTilstander(1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertNotNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
    }
}
