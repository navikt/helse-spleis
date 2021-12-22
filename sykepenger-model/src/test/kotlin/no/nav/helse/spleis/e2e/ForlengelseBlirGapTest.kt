package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ForlengelseBlirGapTest : AbstractEndToEndTest() {

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt sykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent), Arbeid(18.januar, 21.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
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
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_GAP
        )
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!)
        assertEquals(INNTEKT, grunnlagsdataInspektør.sammenligningsgrunnlag)
        assertNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent), Arbeid(18.januar, 21.januar))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
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
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent), Arbeid(18.januar, 21.januar))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
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
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_GAP
        )
    }

    @Test
    fun `Bytter fra forlengelse til gap-state når tidligere tilstøtende periodes søknad avsluttes med arbeidsdager - andre periode har mottatt søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent), Arbeid(18.januar, 21.januar))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        inspektør.also {
            assertNoErrors(it)
            assertActivities(it)
        }
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
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
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG,
            AVVENTER_HISTORIKK
        )
    }
}
