package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereForlengelserTest : AbstractEndToEndTest() {

    @Test
    fun `Tillater forlengelse av flere arbeidsgivere`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereForlengelserTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereForlengelserTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        this@FlereArbeidsgivereForlengelserTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@FlereArbeidsgivereForlengelserTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        //Forlengelsen starter her
        val forlengelseperiode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive), orgnummer = a2)
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(
                forlengelseperiode.start,
                forlengelseperiode.endInclusive,
                100.prosent
            ), orgnummer = a1
        )
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(
                forlengelseperiode.start,
                forlengelseperiode.endInclusive,
                100.prosent
            ), orgnummer = a2
        )

        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        this@FlereArbeidsgivereForlengelserTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_SIMULERING, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING, orgnummer = a1)

        this@FlereArbeidsgivereForlengelserTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_UTBETALING, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK, orgnummer = a2)

        this@FlereArbeidsgivereForlengelserTest.håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_SIMULERING, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING, orgnummer = a2)
        this@FlereArbeidsgivereForlengelserTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, true, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_UTBETALING, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `Ghost forlenger annen arbeidsgiver - skal gå fint`()  {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), orgnummer = a1, vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@FlereArbeidsgivereForlengelserTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereForlengelserTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterArbeidsgiveropplysninger(listOf(1.februar til 16.februar), orgnummer = a2, vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        this@FlereArbeidsgivereForlengelserTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        this@FlereArbeidsgivereForlengelserTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)

        this@FlereArbeidsgivereForlengelserTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@FlereArbeidsgivereForlengelserTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET, orgnummer = a2)
        assertEquals(1.januar, inspektør(a1).skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
        assertSame(inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode), inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode))
    }

    @Test
    fun `forlengelse av AvsluttetUtenUtbetaling for flere arbeidsgivere skal ikke gå til AvventerHistorikk uten IM for begge arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 12.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 12.januar), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 12.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 12.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a2
        )

        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(13.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(13.januar, 31.januar, 100.prosent), orgnummer = a2)

        assertTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }
}
