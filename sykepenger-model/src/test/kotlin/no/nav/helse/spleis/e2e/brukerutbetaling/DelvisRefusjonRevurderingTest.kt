package no.nav.helse.spleis.e2e.brukerutbetaling

import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertUtbetalingsbeløp
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSkjønnsmessigFastsettelse
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class DelvisRefusjonRevurderingTest : AbstractEndToEndTest() {

    @Test
    fun `korrigerende inntektsmelding med halvering av inntekt setter riktig refusjonsbeløp fra nyeste inntektsmelding`() {
        nyttVedtak(januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 2,
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
        )

        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())

        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        håndterOverstyrInntekt(INNTEKT / 2, skjæringstidspunkt = skjæringstidspunkt)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        this@DelvisRefusjonRevurderingTest.håndterSkjønnsmessigFastsettelse(skjæringstidspunkt, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT / 2)))

        this@DelvisRefusjonRevurderingTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@DelvisRefusjonRevurderingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 715, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 715, 715, subset = 17.januar til 31.januar)
    }

    @Test
    fun `overstyring av inntekt med økning av inntekt uten nytt refusjonsbeløp`() {
        nyttVedtak(januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(50000.månedlig, skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode))
        this@DelvisRefusjonRevurderingTest.håndterYtelser(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `overstyring av inntekt med nedjustering av inntekt uten nytt refusjonsbeløp`() {
        nyttVedtak(januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

        håndterOverstyrInntekt(INNTEKT / 2, skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        this@DelvisRefusjonRevurderingTest.håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT / 2)))

        this@DelvisRefusjonRevurderingTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@DelvisRefusjonRevurderingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 715, 1431, subset = 17.januar til 31.januar)
    }

    @Test
    fun `to arbeidsgivere hvor inntekten på den ene senkes slik at den andre arbeidsgiveren får brukerutbetalinger`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        val a1Inntekt = 50000.månedlig
        val a2Inntekt = 10000.månedlig

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = a1Inntekt,
            refusjon = Inntektsmelding.Refusjon(a1Inntekt, null, emptyList()),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = a2Inntekt,
            refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )

        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@DelvisRefusjonRevurderingTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@DelvisRefusjonRevurderingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        this@DelvisRefusjonRevurderingTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        this@DelvisRefusjonRevurderingTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)

        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 2308,
            subset = 1.januar til 16.januar,
            orgnummer = a1
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 2161,
            forventetArbeidsgiverRefusjonsbeløp = 2308,
            subset = 17.januar til 31.januar,
            orgnummer = a1
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 0,
            subset = 1.januar til 16.januar,
            orgnummer = a2
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 0,
            subset = 17.januar til 31.januar,
            orgnummer = a2
        )

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = a1Inntekt / 2,
            refusjon = Inntektsmelding.Refusjon(a1Inntekt / 2, null, emptyList()),
            orgnummer = a1
        )
        håndterOverstyrInntekt(
            inntekt = a1Inntekt / 2,
            skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
            orgnummer = a1
        )
        this@DelvisRefusjonRevurderingTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsler(listOf(Varselkode.RV_IM_4, Varselkode.RV_UT_23), 1.vedtaksperiode.filter(orgnummer = a1))
        assertSisteTilstand(1.vedtaksperiode, orgnummer = a1, tilstand = AVVENTER_SIMULERING_REVURDERING)
        assertSisteTilstand(1.vedtaksperiode, orgnummer = a2, tilstand = AVVENTER_REVURDERING)
    }
}
