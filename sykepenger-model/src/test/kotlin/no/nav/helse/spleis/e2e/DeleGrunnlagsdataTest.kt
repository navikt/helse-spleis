package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.ForlengelseFraInfotrygd.JA
import no.nav.helse.person.ForlengelseFraInfotrygd.NEI
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

internal class DeleGrunnlagsdataTest : AbstractEndToEndTest() {

    @Test
    fun `vilkårsgrunnlag deles med påfølgende tilstøtende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSykmelding(Sykmeldingsperiode(5.april, 30.april, 100))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)

        assertNotNull(inspektør.vilkårsgrunnlag(0))
        assertEquals(inspektør.vilkårsgrunnlag(0), inspektør.vilkårsgrunnlag(1))
        assertEquals(inspektør.vilkårsgrunnlag(0), inspektør.vilkårsgrunnlag(2))
        assertNull(inspektør.vilkårsgrunnlag(3))
    }

    @Test
    fun `vilkårsgrunnlag hentes fra foregående`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSykmelding(Sykmeldingsperiode(5.april, 30.april, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(5.april, 30.april, 100))

        assertNotNull(inspektør.vilkårsgrunnlag(0))
        assertEquals(inspektør.vilkårsgrunnlag(0), inspektør.vilkårsgrunnlag(1))
        assertEquals(inspektør.vilkårsgrunnlag(0), inspektør.vilkårsgrunnlag(2))
        assertNull(inspektør.vilkårsgrunnlag(3))
    }

    @Test
    fun `vilkårsgrunnlag hentes ikke når perioden er overgang fra IT`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100))
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000, 100, ORGNUMMER))
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000, 100, ORGNUMMER))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertNull(inspektør.vilkårsgrunnlag(0))
        assertNull(inspektør.vilkårsgrunnlag(1))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(0))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(1))
    }

    @Test
    fun `vilkårsgrunnlag hentes når perioden ikke er infotrygdforlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100), Arbeid(25.februar, 28.februar))
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000, 100, ORGNUMMER))
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000, 100, ORGNUMMER))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
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
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP
        )
        assertNull(inspektør.vilkårsgrunnlag(0))
        assertNull(inspektør.vilkårsgrunnlag(1))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(0))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(1))
    }

    @Test
    fun `inntektsmelding bryter ikke opp forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(18.januar, 1.februar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(18.januar, 1.februar)), førsteFraværsdag = 4.mars)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode,
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
        assertTilstander(2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
        assertEquals(inspektør.vilkårsgrunnlag(0), inspektør.vilkårsgrunnlag(1))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(0))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(1))
        assertEquals(18.januar, inspektør.skjæringstidspunkt(0))
        assertEquals(18.januar, inspektør.skjæringstidspunkt(1))
    }

    @Test
    fun `inntektsmelding bryter opp infotrygdforlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000, 100, ORGNUMMER))
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 15000, 100, ORGNUMMER))
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100), Arbeid(1.mars, 3.mars))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(18.januar, 1.februar)), førsteFraværsdag = 4.mars)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
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
            AVVENTER_HISTORIKK
        )
        assertEquals(inspektør.vilkårsgrunnlag(0), inspektør.vilkårsgrunnlag(1))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(0))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(1))
        assertEquals(1.desember(2017), inspektør.skjæringstidspunkt(0))
        assertEquals(1.desember(2017), inspektør.skjæringstidspunkt(1))
    }

    @Test
    fun `vilkårsgrunnlag tilbakestilles når vi ikke er en forlengelse likevel`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100),
            Arbeid(18.januar, 21.januar)
        )
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode) // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
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
            AVVENTER_VILKÅRSPRØVING_GAP
        )
        assertNotNull(inspektør.vilkårsgrunnlag(0))
        assertNull(inspektør.vilkårsgrunnlag(1))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(0))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(1))
    }

    @Test
    fun `får egen vilkårsgrunnlag selv når inntektsmelding overlapper, men er gap mellom periodene`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 4.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 15.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 4.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 15.januar, 100))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 15.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
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
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertNotNull(inspektør.vilkårsgrunnlag(0))
        assertNotNull(inspektør.vilkårsgrunnlag(1))
        assertNotEquals(inspektør.vilkårsgrunnlag(0), inspektør.vilkårsgrunnlag(1))
    }
}
