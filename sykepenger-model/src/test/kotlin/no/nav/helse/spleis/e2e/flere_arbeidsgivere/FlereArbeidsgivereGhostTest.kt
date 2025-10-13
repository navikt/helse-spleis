package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.hentFeltFraBehov
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.lørdag
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Venteårsak.Companion.INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.VedtaksperiodeVenterTest.Companion.assertVenter
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterAvbrytSøknad
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsforhold
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class FlereArbeidsgivereGhostTest : AbstractEndToEndTest() {

    @Test
    fun `bruker avbryter søknad for én arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        håndterAvbrytSøknad(januar, a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `Søknad og IM fra ghost etter kort gap til A1 - så kommer søknad fra A1 som tetter gapet - da gjenbruker vi tidsnære opplysninger`() {
        val ghost = a2
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        tilGodkjenning(10.februar til 28.februar, ghost)
        assertEquals(10.februar, inspektør(ghost).vedtaksperioder(1.vedtaksperiode).inspektør.skjæringstidspunkt)

        nullstillTilstandsendringer()

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        assertEquals(1.januar, inspektør(ghost).vedtaksperioder(1.vedtaksperiode).inspektør.skjæringstidspunkt)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = ghost)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a1)
    }

    @Test
    fun `blir syk fra ghost man har vært syk fra tidligere, og inntektsmeldingen kommer først`() {
        nyeVedtak(1.januar(2017) til 31.januar(2017), a1, a2)

        håndterSøknad(Sykdom(1.januar, fredag den 26.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 2.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

            håndterInntektsmelding(
            listOf(mandag den 29.januar til 13.februar),
            orgnummer = a2
        )
        // IM replayes, og ettersom 27. og 28 blir friskedager pga. IM beregnes skjæringstidspunktet til 29.januar. Når A1 sin søknad kommer dekker den "hullet" med sykdom slik at skjæringstidspunktet blir 1.januar
        observatør.vedtaksperiodeVenter.clear()
        håndterSøknad(Sykdom(lørdag den 27.januar, 20.februar, 100.prosent), orgnummer = a2)
        observatør.assertVenter(2.vedtaksperiode.id(a2), venterPåHva = INNTEKTSMELDING)

        assertEquals(29.januar, inspektør(a2).vedtaksperioder(2.vedtaksperiode).inspektør.skjæringstidspunkt)
        håndterSøknad(Sykdom(lørdag den 27.januar, 20.februar, 100.prosent), orgnummer = a1)
        assertEquals(1.januar, inspektør(a2).vedtaksperioder(2.vedtaksperiode).inspektør.skjæringstidspunkt)

        assertTrue(inspektør(a2).refusjon(1.vedtaksperiode).isNotEmpty())
        assertTrue(inspektør(a2).refusjon(2.vedtaksperiode).isNotEmpty())
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a1)
    }

    @Test
    fun `Ghost som sender IM hvor de opplyser om ikke fravær - men blir syk fra ghost etterpå allikevel`() {
        val ghost = a2

        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1)

        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 2.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertBeregningsgrunnlag(INNTEKT * 2)
            assertSykepengegrunnlag(561804.årlig)
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        // Inntektsmelding fra Ghost vi egentlig ikke trenger, men de sender den allikevel og opplyser om IkkeFravaer...
        // denne vinner over skatt i inntektsturnering
        val ghostIM = håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = 33000.månedlig,
            orgnummer = ghost,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFravaer"
        ).let { MeldingsreferanseId(it) }
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertBeregningsgrunnlag(INNTEKT * 2)
            assertSykepengegrunnlag(561804.årlig)
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        // Så kjem søknaden på ghosten læll
        val ghostSøknad = MeldingsreferanseId(håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ghost))

        assertVarsel(RV_IM_4, 1.vedtaksperiode.filter(orgnummer = ghost))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = ghost)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertBeregningsgrunnlag(INNTEKT + 31_000.månedlig)
            assertSykepengegrunnlag(561804.årlig)
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, 31_000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = ghost)
        håndterSimulering(1.vedtaksperiode, orgnummer = ghost)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = ghost)
        håndterUtbetalt(orgnummer = ghost)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, ghost)

        assertBeløpstidslinje(inspektør(a2).vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, 33000.månedlig, ghostIM.id)
        assertEquals(setOf(Dokumentsporing.søknad(ghostSøknad)), inspektør(ghost).hendelser(1.vedtaksperiode))
    }

    @Test
    fun `blir syk fra ghost`()  {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        nullstillTilstandsendringer()
        håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter(orgnummer = a1))
        assertVarsler(emptyList(), 1.vedtaksperiode.filter(orgnummer = a2))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        nullstillTilstandsendringer()
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `blir syk fra ghost og inntektsmeldingen kommer først`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        nullstillTilstandsendringer()

        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            orgnummer = a2
        )
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `blir syk fra ghost annen måned enn skjæringstidspunkt etter at saksbehandler har overstyrt inntekten etter 8-28, 3 ledd bokstav b -- Ghost sender klassisk inntektsmelding`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(
            1.vedtaksperiode,
            a1, a2,
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a2, INNTEKT * 1.1)))
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.1, forventetKorrigertInntekt = INNTEKT * 1.1, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            beregnetInntekt = INNTEKT,
            orgnummer = a2
        )

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.1, forventetKorrigertInntekt = INNTEKT * 1.1, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
    }

    @Test
    fun `blir syk fra ghost annen måned enn skjæringstidspunkt etter at saksbehandler har overstyrt inntekten etter 8-28, 3 ledd bokstav b -- Ghost svarer på etterspurte arbeidsgiveropplysninger`()  {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(
            1.vedtaksperiode,
            a1, a2,
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a2, INNTEKT * 1.1)))
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.1, forventetKorrigertInntekt = INNTEKT * 1.1, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            beregnetInntekt = INNTEKT,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT)
        }

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
    }

    @Test
    fun `Korrigerende refusjonsopplysninger på arbeidsgiver med skatteinntekt i sykepengegrunnlaget`()  {
        utbetalPeriodeMedGhost()
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        // Arbeidsgiver har fylt inn inntekt for skjæringstidspunktet 1.januar så da vinner det over skatteopplysningene
        val inntektsmelding = håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT)
        }

        assertBeløpstidslinje(Beløpstidslinje.fra(februar, INNTEKT, inntektsmelding.arbeidsgiver), inspektør(a2).refusjon(1.vedtaksperiode))

        val korrigerendeInntektsmelding =  håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.februar til 16.februar),
            førsteFraværsdag = 20.februar,
            orgnummer = a2
        )

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT)
        }
        assertBeløpstidslinje(
            Beløpstidslinje.fra(1.februar til 19.februar, INNTEKT, inntektsmelding.arbeidsgiver) + Beløpstidslinje.fra(20.februar til 28.februar, INNTEKT, korrigerendeInntektsmelding.arbeidsgiver),
            inspektør(a2).refusjon(1.vedtaksperiode)
        )
    }

    @Test
    fun `ghost n stuff`() {
        utbetalPeriodeMedGhost()

        val a1Linje = inspektør(a1).utbetaling(0).arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1080, a1Linje.beløp)
    }

    @Test
    fun `ny ghost etter tidligere ghostperiode`() {
        utbetalPeriodeMedGhost()

        håndterSykmelding(Sykmeldingsperiode(26.mars, 10.april), orgnummer = a1)
        håndterSøknad(Sykdom(26.mars, 10.april, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 26.mars,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 2.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val førsteOppdrag = inspektør(a1).utbetaling(0).arbeidsgiverOppdrag
        val a1Linje = førsteOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1080, a1Linje.beløp)

        val andreOppdrag = inspektør(a1).utbetaling(1).arbeidsgiverOppdrag
        val a1Linje2 = andreOppdrag.single()
        assertEquals(26.mars, a1Linje2.fom)
        assertEquals(10.april, a1Linje2.tom)
        assertEquals(1080, a1Linje2.beløp)
    }

    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som starter etter skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 2.januar, null)
            ),
            orgnummer = a1
        )
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }

        val a1Linje = inspektør(a1).utbetaling(0).arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1431, a1Linje.beløp)
    }

    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som slutter før skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            månedligeInntekter = mapOf(
                oktober(2017) to listOf(a1 to INNTEKT),
                november(2017) to listOf(a1 to INNTEKT),
                desember(2017) to listOf(a1 to INNTEKT, a2 to INNTEKT)
            ),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 1.desember(2017), 31.desember(2017))
            ),
            orgnummer = a1
        )
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }

        val a1Linje = inspektør(a1).utbetaling(0).arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1431, a1Linje.beløp)
    }

    @Test
    fun `Tar ikke med arbeidsforhold dersom personen startet i jobb mer enn 2 måneder før skjæringstidspunktet og ikke har inntekt de 2 siste månedene`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 31.desember(2017), null)
            ),
            orgnummer = a1
        )
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }
    }

    @Test
    fun `Tar med arbeidsforhold dersom personen startet i jobb mindre enn 2 måneder før skjæringstidspunktet, selvom det mangler inntekt de 2 siste månedene`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 2.januar, null)
            ),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }
    }

    @Test
    fun `Tar ikke med arbeidsforhold dersom siste inntekt var 3 måneder før skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            månedligeInntekter = mapOf(
                desember(2017) to listOf(a1 to INNTEKT, a2 to INNTEKT),
                januar(2018) to listOf(a1 to INNTEKT),
                februar(2018) to listOf(a1 to INNTEKT),
            ),
            orgnummer = a1
        )
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }
    }

    @Test
    fun `bruker har fyllt inn andre inntektskilder i søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)

        håndterSøknad(
            Sykdom(1.mars, 31.mars, 100.prosent),
            andreInntektskilder = true,
            orgnummer = a1
        )
        assertFunksjonellFeil(RV_SØ_10)
    }

    @Test
    fun `Forlengelse av en ghostsak skal ikke få warning - stoler på avgjørelsen som ble tatt i førstegangsbehandlingen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
        assertVarsler(emptyList(), 2.vedtaksperiode.filter(a1))
    }

    @Test
    fun `tar med arbeidsforhold i vilkårsgrunnlag som startet innen 2 mnd før skjæringstidspunkt, selvom vi ikke har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 1.november(2017), null)
            ),
            orgnummer = a1
        )
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, 30_000.månedlig)
            assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `overstyrer inntekt dersom det ikke er rapportert inn inntekt enda`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "Jeg, en saksbehandler, overstyrte pga 8-15")))
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, 30_000.månedlig)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
        }
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `skal ikke gå til AvventerHistorikk uten IM fra alle arbeidsgivere om vi ikke overlapper med første vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(18.januar, 10.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(18.januar, 10.februar), orgnummer = a2)
        håndterSøknad(Sykdom(18.januar, 10.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(18.januar, 10.februar, 100.prosent), orgnummer = a2)
        håndterArbeidsgiveropplysninger(
            listOf(18.januar til 2.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `forlengelse av ghost med IM som har første fraværsdag på annen måned enn skjæringstidspunkt skal ikke vente på IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 12.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 12.februar), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 12.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 12.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            orgnummer = a2
        )

        håndterSykmelding(Sykmeldingsperiode(13.februar, 28.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(13.februar, 28.februar), orgnummer = a2)
        håndterSøknad(Sykdom(13.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(13.februar, 28.februar, 100.prosent), orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `forlengelse av ghost med IM som har første fraværsdag på annen måned enn skjæringstidspunkt skal ikke vente på IM (uferdig)`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        håndterSykmelding(Sykmeldingsperiode(21.februar, 28.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(21.februar, 28.februar), orgnummer = a2)
        håndterSøknad(Sykdom(21.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(21.februar, 28.februar, 100.prosent), orgnummer = a2)


        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `deaktivert arbeidsforhold blir med i vilkårsgrunnlag`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 1.desember(2017), null)
            )
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        val relevanteOrgnumre1: Iterable<String> = hendelselogg.hentFeltFraBehov(1.vedtaksperiode.id(a1), Behovtype.Godkjenning, "orgnummereMedRelevanteArbeidsforhold") ?: fail { "forventet orgnummereMedRelevanteArbeidsforhold" }
        assertEquals(listOf(a1, a2).toList(), relevanteOrgnumre1.toList())
        this@FlereArbeidsgivereGhostTest.håndterOverstyrArbeidsforhold(
            skjæringstidspunkt, listOf(
            OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(
                a2,
                true,
                "forklaring"
            )
        )
        )
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val relevanteOrgnumre2: Iterable<String> = hendelselogg.hentFeltFraBehov(1.vedtaksperiode.id(a1), Behovtype.Godkjenning, "orgnummereMedRelevanteArbeidsforhold") ?: fail { "forventet orgnummereMedRelevanteArbeidsforhold" }
        assertEquals(listOf(a1), relevanteOrgnumre2.toList())
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertBeregningsgrunnlag(372000.årlig)
            assertSykepengegrunnlag(372000.årlig)
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
        }
    }

    @Test
    fun `arbeidsgiver går fra å være ghost mens første arbeidsgiver står til godkjenning -- Ghost sender klassisk inntektsmelding`() {
        utbetalPeriodeMedGhost(tilGodkjenning = true)

        nyPeriode(16.mars til 31.mars, a1) // Forlengelse på a1
        nyPeriode(16.mars til 15.april, a2) // Går fra Ghost -> ikke ghost

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, a2)

        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        // IM for tidligere ghost a2 sparker igang revurdering på a1
        håndterInntektsmelding(
            listOf(16.mars til 31.mars),
            førsteFraværsdag = 16.mars,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
            orgnummer = a2
        )

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a2)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        // Her står saken nå (*NÅ*)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)

        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, a1)

        // Vi har fortsatt ikke beregnet hva utbetalingen for a2 kommer til å bli, men saksbehandleren lurer på det allerede *NÅ*
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, a2)
    }

    @Test
    fun `arbeidsgiver går fra å være ghost mens første arbeidsgiver står til godkjenning -- Ghost svarer på etterspurte arbeidsgiveropplysninger`()  {
        utbetalPeriodeMedGhost(tilGodkjenning = true)

        nyPeriode(16.mars til 31.mars, a1) // Forlengelse på a1
        nyPeriode(16.mars til 15.april, a2) // Går fra Ghost -> ikke ghost

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, a2)

        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        // IM for tidligere ghost a2 sparker igang revurdering på a1
        håndterArbeidsgiveropplysninger(
            listOf(16.mars til 31.mars),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a2)

        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT)
        }
    }

    @Test
    fun `deaktivere arbeidsgiver med kort søknad`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.januar, 50.prosent), orgnummer = a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertEquals(75, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[1.januar].økonomi.inspektør.totalGrad)
        this@FlereArbeidsgivereGhostTest.håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
    }

    private fun utbetalPeriodeMedGhost(tilGodkjenning: Boolean = false) {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))
        this@FlereArbeidsgivereGhostTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        if (tilGodkjenning) return
        this@FlereArbeidsgivereGhostTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
    }
}
