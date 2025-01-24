package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.util.*
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.OverstyringIgangsatt.VedtaksperiodeData
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FlereUkjenteArbeidsgivereTest : AbstractEndToEndTest() {

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandlingen hos ag2`() {
        val inntektA1 = INNTEKT + 500.daglig
        val inntektA2 = INNTEKT

        nyeVedtak(januar, a1, a2, inntekt = INNTEKT)
        forlengVedtak(februar, orgnummer = a1)
        forlengVedtak(mars, orgnummer = a1)

        val im1 = håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = inntektA1,
            orgnummer = a1
        )
        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter(a1))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter(orgnummer = a2))

        val im2 = håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            beregnetInntekt = inntektA2,
            orgnummer = a2
        )
        nullstillTilstandsendringer()
        val søknad = UUID.randomUUID()
        håndterSøknad(Sykdom(1.mars, 20.mars, 100.prosent), id = søknad, orgnummer = a2)

        assertInntektsgrunnlag(1.januar, a1, inntektA1, inntektA1)
        assertInntektsgrunnlag(1.januar, a2, inntektA2, inntektA2)

        val overstyringerIgangsatt = observatør.overstyringIgangsatt
        assertEquals(3, overstyringerIgangsatt.size)

        overstyringerIgangsatt[0].also { event ->
            assertEquals(
                PersonObserver.OverstyringIgangsatt(
                    årsak = "KORRIGERT_INNTEKTSMELDING_INNTEKTSOPPLYSNINGER",
                    skjæringstidspunkt = 1.januar,
                    periodeForEndring = 1.januar til 1.januar,
                    berørtePerioder = listOf(
                        VedtaksperiodeData(a1, 1.vedtaksperiode.id(a1), 1.januar til 31.januar, 1.januar, "REVURDERING"),
                        VedtaksperiodeData(a1, 2.vedtaksperiode.id(a1), februar, 1.januar, "REVURDERING"),
                        VedtaksperiodeData(a1, 3.vedtaksperiode.id(a1), 1.mars til 31.mars, 1.januar, "REVURDERING"),
                        VedtaksperiodeData(a2, 1.vedtaksperiode.id(a2), 1.januar til 31.januar, 1.januar, "REVURDERING")
                    ),
                    meldingsreferanseId = im1
                ), event
            )
        }

        overstyringerIgangsatt[1].also { event ->
            assertEquals(
                PersonObserver.OverstyringIgangsatt(
                    årsak = "NY_PERIODE",
                    skjæringstidspunkt = 1.januar,
                    periodeForEndring = 1.mars til 20.mars,
                    berørtePerioder = listOf(
                        VedtaksperiodeData(a1, 3.vedtaksperiode.id(a1), 1.mars til 31.mars, 1.januar, "REVURDERING")
                    ),
                    meldingsreferanseId = søknad
                ), event
            )
        }

        overstyringerIgangsatt[2].also { event ->
            assertEquals(
                PersonObserver.OverstyringIgangsatt(
                    årsak = "ARBEIDSGIVERPERIODE",
                    skjæringstidspunkt = 1.mars,
                    periodeForEndring = 1.mars til 20.mars,
                    berørtePerioder = listOf(
                        VedtaksperiodeData(a1, 3.vedtaksperiode.id(a1), 1.mars til 31.mars, 1.januar, "REVURDERING")
                    ),
                    meldingsreferanseId = im2
                ), event
            )
        }

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandling hos ag2 som først var ansett som ghost`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        nullstillTilstandsendringer()

        // a2 sent til festen
        val id = håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a2,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ja"
        )
        assertEquals(id, observatør.inntektsmeldingIkkeHåndtert.single())
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter(a1))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter(a2))
        assertTilstander(
            1.vedtaksperiode,
            AVVENTER_GODKJENNING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING, orgnummer = a1
        )
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandling hos ag2 som først var ansett som ghost - a1 revurderes`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()
        nullstillTilstandsendringer()

        // a2 sent til festen
        val imId = håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a2,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ja"
        )
        assertEquals(imId, observatør.inntektsmeldingIkkeHåndtert.single())
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        val søknadId = håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter(a1))

        assertBeløpstidslinje(inspektør(a2).vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INNTEKT, imId)

        assertEquals(
            setOf(
                Dokumentsporing.søknad(søknadId),
                Dokumentsporing.inntektsmeldingDager(imId),
                Dokumentsporing.inntektsmeldingInntekt(imId)
            ), inspektør(a2).hendelser(1.vedtaksperiode)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter(a2))
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
            orgnummer = a1
        )
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandling hos ag2 som først var antatt å være frisk - men tidlig inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        val imId = håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a2,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ja"
        )
        assertEquals(imId, observatør.inntektsmeldingIkkeHåndtert.single())

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()
        nullstillTilstandsendringer()

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2) // a2 sent til festen, men med ting liggende i vilkårsgrunnlaget
        val søknadId = håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter(a1))

        assertInntektsgrunnlag(1.januar, a1, INNTEKT)
        assertInntektsgrunnlag(1.januar, a2, INNTEKT)
        assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, imId.arbeidsgiver), inspektør(a2).refusjon(1.vedtaksperiode))

        assertEquals(
            setOf(
                Dokumentsporing.søknad(søknadId),
                Dokumentsporing.inntektsmeldingDager(imId),
                Dokumentsporing.inntektsmeldingInntekt(imId)
            ), inspektør(a2).hendelser(1.vedtaksperiode)
        )
        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter(a2))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
            orgnummer = a1
        )
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a2)
    }
}
