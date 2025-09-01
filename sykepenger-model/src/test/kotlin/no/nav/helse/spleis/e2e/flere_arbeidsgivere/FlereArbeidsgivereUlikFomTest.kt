package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.a4
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.person.VedtaksperiodeVenter
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykepengegrunnlagForArbeidsgiver
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.søndag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import no.nav.helse.økonomi.inspectors.inspektør

internal class FlereArbeidsgivereUlikFomTest : AbstractEndToEndTest() {

    @Test
    fun `Gjenbruk av tidsnære opplysninger slår ikke til ved skatteinntekt i inntektsgrunnlaget`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        håndterSøknad(februar, orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            orgnummer = a2
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        val refusjonFør = inspektør(a2).refusjon(1.vedtaksperiode)
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        observatør.vedtaksperiodeVenter.clear()
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.februar, Dagtype.Arbeidsdag)), orgnummer = a2)
        assertEquals(emptyList<VedtaksperiodeVenter>(), observatør.vedtaksperiodeVenter)
        håndterSykepengegrunnlagForArbeidsgiver(skjæringstidspunkt = 2.februar, orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertEquals(refusjonFør, inspektør(a2).refusjon(1.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a2)
        assertVarsler(listOf(RV_IV_10), 1.vedtaksperiode.filter(a2))
    }

    @Test
    fun `Går ikke videre til vilkårsprøving om vi mangler IM fra en arbeidsgiver`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 31.mars, 100.prosent), orgnummer = a3)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
    }

    @Test
    fun `kort periode hos en ag2 forkaster utbetaling`() {
        nyPeriode(1.januar til 20.januar, a1)
        nyPeriode(5.januar til 20.januar, a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertEquals(1, inspektør(a1).utbetalinger(1.vedtaksperiode).size)
        assertTrue(inspektør(a2).utbetalinger(1.vedtaksperiode).isEmpty())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
    }

    @Test
    fun `ag2 forkaster ikke utbetaling tildelt av ag1`() {
        nyPeriode(1.januar til 20.januar, a1)
        nyPeriode(1.januar til 20.januar, a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 2,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 2,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        håndterYtelser(orgnummer = a2)
        assertEquals(1, inspektør(a1).utbetalinger(1.vedtaksperiode).size)
        val ag2Utbetalinger = inspektør(a2).utbetalinger(1.vedtaksperiode)
        assertEquals(1, ag2Utbetalinger.size)
        assertEquals(Utbetalingstatus.IKKE_UTBETALT, ag2Utbetalinger[0].inspektør.tilstand)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a2)
    }

    @Test
    fun `ag2 forkaster utbetaling tildelt av ag1 om det har skjedd endringer i mellomtiden`() {
        nyPeriode(1.januar til 20.januar, a1)
        nyPeriode(1.januar til 20.januar, a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 2,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 2,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Dagtype.Feriedag)), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        håndterYtelser(orgnummer = a2)
        assertEquals(2, inspektør(a1).utbetalinger(1.vedtaksperiode).size)
        val ag2Utbetalinger = inspektør(a2).utbetalinger(1.vedtaksperiode)
        assertEquals(2, ag2Utbetalinger.size)
        assertEquals(Utbetalingstatus.FORKASTET, ag2Utbetalinger[0].inspektør.tilstand)
        assertEquals(Utbetalingstatus.IKKE_UTBETALT, ag2Utbetalinger[1].inspektør.tilstand)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a2)
    }

    @Test
    fun `To førstegangsbehandlinger med ulik fom i forskjellige måneder - skal bruke skatteinntekter for arbeidsgiver med senest fom`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars), orgnummer = a2)

        håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            orgnummer = a1
        ) // ff 1 mars
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = INNTEKT,
            orgnummer = a2
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT, a2 to 20000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        assertInntektsgrunnlag(28.februar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, 20000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }
    }

    @Test
    fun `To førstegangsbehandlinger med lik fom - skal bruke inntektsmelding for begge arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            beregnetInntekt = 18000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )

        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, 30000.månedlig)
            assertInntektsgrunnlag(a2, 18000.månedlig)
        }
    }

    @Test
    fun `Bruker gjennomsnitt av skatteinntekter ved ulik fom i forskjellige måneder`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars), orgnummer = a2)

        håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = INNTEKT,
            orgnummer = a2
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 31000.månedlig, a2 to 21000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        assertInntektsgrunnlag(28.februar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, 21000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }
    }

    @Test
    fun `Ulik fom og ikke 6G-begrenset, utbetalinger beregnes riktig`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars), orgnummer = a2)

        håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = 20000.månedlig,
            orgnummer = a2
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 10000.månedlig, a2 to 20000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(16.mars, a1Linje.fom)
        assertEquals(30.mars, a1Linje.tom)
        assertEquals(462, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(21.mars, a2Linje.fom)
        assertEquals(30.mars, a2Linje.tom)
        assertEquals(20000.månedlig.dagligInt, a2Linje.beløp)
    }

    @Test
    fun `Ulik fom og 6G-begrenset, skal beregne utbetaling ut fra skatteinntekter for a2`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars), orgnummer = a2)
        håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val arbeidsgiverOppdrag = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag
        assertEquals(1, arbeidsgiverOppdrag.size)
        val a1Linje1 = arbeidsgiverOppdrag[0]
        assertEquals(16.mars, a1Linje1.fom)
        assertEquals(30.mars, a1Linje1.tom)
        assertEquals(997, a1Linje1.beløp)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(21.mars, a2Linje.fom)
        assertEquals(30.mars, a2Linje.tom)
        assertEquals(1163, a2Linje.beløp)
    }

    @Test
    fun `Førstegangsbehandling med ulik fom og siste arbeidsgiver er 50 prosent sykmeldt`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars), orgnummer = a2)
        håndterSøknad(Sykdom(5.mars, 31.mars, 50.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)


        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a1Linje = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(16.mars, a1Linje.fom)
        assertEquals(30.mars, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(21.mars, a2Linje.fom)
        assertEquals(30.mars, a2Linje.tom)
        assertEquals(582, a2Linje.beløp)
    }

    @Test
    fun `mursteinspølser og totalgrad`() {
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 10,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            skatteinntekter = listOf(a1 to INNTEKT / 10, a2 to INNTEKT)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        assertEquals(2, inspektør(a1).antallUtbetalinger)
        assertEquals(2, inspektør(a2).antallUtbetalinger)

        assertEquals(100, inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode)[31.januar].økonomi.inspektør.totalGrad)
        assertEquals(100, inspektør(a2).utbetalingstidslinjer(2.vedtaksperiode)[1.februar].økonomi.inspektør.totalGrad)
    }

    @Test
    fun `mursteinspølser og manglende inntektsmelding på a2`() {
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        håndterSøknad(Sykdom(5.januar, søndag den 21.januar, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        nullstillTilstandsendringer()
        // Perioden som står i AvventerGodkjenningRevurdering får ikke noe signal om overstyring igangsatt ettersom disse periodene er _etter_ 1.vedtaksperiode
        // Allikevel kan den ikke beregnes på nytt, fordi mursteinssituasjonen som nå har oppstått gjør at vi mangler refusjonsopplysninger på 22.januar
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        håndterSøknad(Sykdom(21.januar, 5.februar, 100.prosent), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)

        // Om f.eks. saksbehandler overstyrer perioden som står i AvventerGodkjenningRevurdering blir den sittende fast i AvventerRevurdering
        // Ettersom vi nå må ha inntektsmelding fra a2 for refusjonsopplysninger 22.januar
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1)))

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, orgnummer = a1)
        val venterPå = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) }.venterPå
        assertEquals("INNTEKTSMELDING", venterPå.venteårsak.hva)
        assertEquals(a2, venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer)
        assertEquals(2.vedtaksperiode.id(a2), venterPå.vedtaksperiodeId)
    }

    @Test
    fun `Førstegangsbehandling med ulik fom og første arbeidsgiver er 50 prosent sykmeldt`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(28.februar, 31.mars, 50.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars), orgnummer = a2)
        håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)


        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a1Linje = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(16.mars, a1Linje.fom)
        assertEquals(30.mars, a1Linje.tom)
        assertEquals(499, a1Linje.beløp)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(21.mars, a2Linje.fom)
        assertEquals(30.mars, a2Linje.tom)
        assertEquals(1163, a2Linje.beløp)
    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars), orgnummer = a2)
        håndterSøknad(Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 28.februar), orgnummer = a3)
        håndterSøknad(Sykdom(3.januar, 28.februar, 100.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.februar), orgnummer = a4)
        håndterSøknad(Sykdom(4.januar, 15.februar, 100.prosent), orgnummer = a4)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(2.januar til 17.januar),
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(3.januar til 18.januar),
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(4.januar til 19.januar),
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )

        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3, a4, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        val a1Linje = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(31.januar, a1Linje.tom)
        assertEquals(515, a1Linje.beløp)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(18.januar, a2Linje.fom)
        assertEquals(15.mars, a2Linje.tom)
        assertEquals(532, a2Linje.beløp)

        assertEquals(1, inspektør(a3).sisteUtbetaling().arbeidsgiverOppdrag.size)
        val a3Linje1 = inspektør(a3).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(19.januar, a3Linje1.fom)
        assertEquals(28.februar, a3Linje1.tom)
        assertEquals(549, a3Linje1.beløp)

        val a4Linje = inspektør(a4).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(20.januar, a4Linje.fom)
        assertEquals(15.februar, a4Linje.tom)
        assertEquals(565, a4Linje.beløp)
    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars), orgnummer = a2)
        håndterSøknad(Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars), orgnummer = a3)
        håndterSøknad(Sykdom(3.januar, 15.mars, 100.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars), orgnummer = a4)
        håndterSøknad(Sykdom(4.januar, 15.mars, 100.prosent), orgnummer = a4)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(2.januar til 17.januar),
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(3.januar til 18.januar),
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(4.januar til 19.januar),
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3, a4, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        val a1Linje = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(515, a1Linje.beløp)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(18.januar, a2Linje.fom)
        assertEquals(15.mars, a2Linje.tom)
        assertEquals(532, a2Linje.beløp)

        val a3Linje = inspektør(a3).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(19.januar, a3Linje.fom)
        assertEquals(15.mars, a3Linje.tom)
        assertEquals(549, a3Linje.beløp)

        val a4Linje = inspektør(a4).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(20.januar, a4Linje.fom)
        assertEquals(15.mars, a4Linje.tom)
        assertEquals(565, a4Linje.beløp)
    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt, nå med gradert sykmelding!`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars), orgnummer = a2)
        håndterSøknad(Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars), orgnummer = a3)
        håndterSøknad(Sykdom(3.januar, 15.mars, 50.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars), orgnummer = a4)
        håndterSøknad(Sykdom(4.januar, 15.mars, 100.prosent), orgnummer = a4)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(2.januar til 17.januar),
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(3.januar til 18.januar),
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(4.januar til 19.januar),
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3, a4, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        val a1Linjer = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag
        assertEquals(1, a1Linjer.size)
        assertEquals(17.januar, a1Linjer[0].fom)
        assertEquals(15.mars, a1Linjer[0].tom)
        assertEquals(515, a1Linjer[0].beløp)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(18.januar, a2Linje.fom)
        assertEquals(15.mars, a2Linje.tom)
        assertEquals(532, a2Linje.beløp)

        val arbeidsgiver3Oppdrag = inspektør(a3).sisteUtbetaling().arbeidsgiverOppdrag
        assertEquals(1, arbeidsgiver3Oppdrag.size)
        val a3Linje = arbeidsgiver3Oppdrag.single()
        assertEquals(19.januar, a3Linje.fom)
        assertEquals(15.mars, a3Linje.tom)
        assertEquals(274, a3Linje.beløp)

        val a4Linje = inspektør(a4).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(20.januar, a4Linje.fom)
        assertEquals(15.mars, a4Linje.tom)
        assertEquals(565, a4Linje.beløp)
    }

    @Test
    fun `Wow! Her var det mye greier - ulik fom, lik tom, forskjellig gradering for alle arbeidsgivere`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 22.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars), orgnummer = a2)
        håndterSøknad(Sykdom(2.januar, 15.mars, 69.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars), orgnummer = a3)
        håndterSøknad(Sykdom(3.januar, 15.mars, 42.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars), orgnummer = a4)
        håndterSøknad(Sykdom(4.januar, 15.mars, 37.prosent), orgnummer = a4)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(2.januar til 17.januar),
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(3.januar til 18.januar),
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(4.januar til 19.januar),
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3, a4, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        val a1Linje = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(113, a1Linje.beløp)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(18.januar, a2Linje.fom)
        assertEquals(15.mars, a2Linje.tom)
        assertEquals(367, a2Linje.beløp)

        val a3Linje = inspektør(a3).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(19.januar, a3Linje.fom)
        assertEquals(15.mars, a3Linje.tom)
        assertEquals(230, a3Linje.beløp)

        val a4Linje = inspektør(a4).sisteUtbetaling().arbeidsgiverOppdrag.single()
        assertEquals(20.januar, a4Linje.fom)
        assertEquals(15.mars, a4Linje.tom)
        assertEquals(209, a4Linje.beløp)
    }

    @Test
    fun `Flere arbeidsgivere med ulik fom - skal få warning om flere arbeidsforhold med ulikt sykefravær`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(4.mars, 31.mars), orgnummer = a2)
        håndterSøknad(Sykdom(4.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )

        håndterInntektsmelding(
            listOf(4.mars til 19.mars),
            førsteFraværsdag = 4.mars,
            beregnetInntekt = 19000.månedlig,
            orgnummer = a2
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, 10000.månedlig)
            assertInntektsgrunnlag(a2, 19000.månedlig)
        }
    }

    @Test
    fun `Flere arbeidsgivere med lik fom - skal ikke få warning om flere arbeidsforhold med ulikt sykefravær`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )

        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            beregnetInntekt = 19000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsler(emptyList(), 1.vedtaksperiode.filter(a1))
        assertVarsler(emptyList(), 1.vedtaksperiode.filter(a2))
    }

    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(20.mars, 25.april), orgnummer = a2)
        håndterSøknad(Sykdom(20.mars, 25.april, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(20.mars til 4.april),
            førsteFraværsdag = 20.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag.last()
        assertEquals(16.mars, a1Linje.fom)
        assertEquals(30.mars, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.last()
        assertEquals(5.april, a2Linje.fom)
        assertEquals(25.april, a2Linje.tom)
        assertEquals(1163, a2Linje.beløp)
    }

    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG, nå med gradert sykmelding!`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(28.februar, 31.mars, 50.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(20.mars, 25.april), orgnummer = a2)
        håndterSøknad(Sykdom(20.mars, 25.april, 70.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(20.mars til 4.april),
            førsteFraværsdag = 20.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).sisteUtbetaling().arbeidsgiverOppdrag.last()
        assertEquals(16.mars, a1Linje.fom)
        assertEquals(30.mars, a1Linje.tom)
        assertEquals(499, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a2Linje = inspektør(a2).sisteUtbetaling().arbeidsgiverOppdrag.last()
        assertEquals(5.april, a2Linje.fom)
        assertEquals(25.april, a2Linje.tom)
        assertEquals(814, a2Linje.beløp)
    }

    @Test
    fun `skjæringstidspunkt i samme måned betyr at begge arbeidsgivere bruker inntekt fra inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars), orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            listOf(5.mars til 20.mars),
            beregnetInntekt = 21000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)

        assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, 21000.månedlig)
        }
    }

    @Test
    fun `skjæringstidspunkt i forskjellige måneder betyr at senere arbeidsgiver bruker skatteinntekt`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 30.mars), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 30.mars), orgnummer = a2)

        håndterSøknad(Sykdom(28.februar, 30.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 30.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            beregnetInntekt = 21000.månedlig,
            orgnummer = a2
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 20000.månedlig),
            orgnummer = a1
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        assertInntektsgrunnlag(28.februar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, 20000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }
    }

    @Test
    fun `To arbeidsgivere med ulik fom i samme måned - med en tidligere periode i samme måned - andre vedtaksperiode velger IM for egen første fraværsdag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 21.januar,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 22.januar,
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2, orgnummer = a1)
        assertInntektsgrunnlag(21.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, 32000.månedlig)
        }
    }

    @Test
    fun `alle arbeidsgivere burde hoppe inn i AVVENTER_BLOKKERENDE_PERIODE dersom de har samme skjæringstidspunkt men ikke overlapper`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar), orgnummer = a3)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a3)
        håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            orgnummer = a3,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a3)
    }

    @Test
    fun `søknad for ghost etter utbetalt`()  {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        forlengVedtak(februar, orgnummer = a1)

        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)

        nullstillTilstandsendringer()
        håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET, orgnummer = a1)

        val revurderingen = inspektør(a1).sisteUtbetaling()
        assertEquals(1, revurderingen.arbeidsgiverOppdrag.size)
        assertEquals(0, revurderingen.personOppdrag.size)
        revurderingen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(1.februar til 28.februar, linje.fom til linje.tom)
            assertEquals(1080, linje.beløp)
        }

        val førstegangsutbetalingen = inspektør(a2).sisteUtbetaling()
        assertEquals(1, førstegangsutbetalingen.arbeidsgiverOppdrag.size)
        assertEquals(0, førstegangsutbetalingen.personOppdrag.size)
        førstegangsutbetalingen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(17.februar til 28.februar, linje.fom til linje.tom)
            assertEquals(1080, linje.beløp)
        }
    }

    @Test
    fun `søknad for ghost etter utbetalt som delvis overlapper med to perioder hos a1`()  {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        forlengVedtak(februar, orgnummer = a1)

        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(20.januar, 28.februar, 100.prosent), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)

        nullstillTilstandsendringer()
        håndterArbeidsgiveropplysninger(
            listOf(20.januar til 4.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET, orgnummer = a1)

        val revurderingen = inspektør(a1).sisteUtbetaling()
        assertEquals(1, revurderingen.arbeidsgiverOppdrag.size)
        assertEquals(0, revurderingen.personOppdrag.size)
        revurderingen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(1.februar til 28.februar, linje.fom til linje.tom)
            assertEquals(1080, linje.beløp)
        }

        val førstegangsutbetalingen = inspektør(a2).sisteUtbetaling()
        assertEquals(1, førstegangsutbetalingen.arbeidsgiverOppdrag.size)
        assertEquals(0, førstegangsutbetalingen.personOppdrag.size)
        førstegangsutbetalingen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(5.februar til 28.februar, linje.fom til linje.tom)
            assertEquals(1080, linje.beløp)
        }
    }

    @Test
    fun `skjæringstidspunktet er i måneden før ag1`() {
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(20.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            orgnummer = a1
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertInntektsgrunnlag(20.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter(orgnummer = a1))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a1)
    }

    @Test
    fun `skjæringstidspunktet er i måneden før ag1 og ag2 - nyoppstartet arbeidsforhold ag2 - bare inntekt 3 mnd før`() {
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(31.januar, 14.februar, 100.prosent), orgnummer = a3)
        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            orgnummer = a2
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to INNTEKT),
            arbeidsforhold = listOf(
                Triple(a1, LocalDate.EPOCH, null),
                Triple(a2, 1.november(2017), null),
                Triple(a3, 1.oktober(2017), null)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertInntektsgrunnlag(31.januar, forventetAntallArbeidsgivere = 3) {
            assertInntektsgrunnlag(a1, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
            assertInntektsgrunnlag(a3, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter(orgnummer = a1))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a1)
    }

    @Test
    fun `avviser dag med totalgrad under 20 prosent`() {
        val inntektA1 = 51775.månedlig
        val inntektA2 = 10911.månedlig
        nyPeriode(1.mai(2023) til 30.mai(2023), orgnummer = a1)
        nyPeriode(1.mai(2023) til 31.mai(2023), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            listOf(1.mai(2023) til 16.mai(2023)),
            beregnetInntekt = inntektA1,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.mai(2023) til 16.mai(2023)), beregnetInntekt = inntektA2,
            refusjon = Refusjon(
                INGEN, null, emptyList()
            ),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )

        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val utbetalingstidslinje = inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode)

        assertEquals(0.daglig, utbetalingstidslinje[31.mai(2023)].økonomi.inspektør.personbeløp)
        assertNotNull(utbetalingstidslinje[31.mai(2023)].erAvvistMed(Begrunnelse.MinimumSykdomsgrad))
        assertEquals(17, utbetalingstidslinje[31.mai(2023)].økonomi.inspektør.totalGrad)
    }
}
