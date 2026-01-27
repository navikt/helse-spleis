package no.nav.helse.spleis.e2e.inntektsmelding

import java.time.LocalDateTime
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSkjønnsmessigFastsettelse
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ReplayInntektsmeldingE2ETest : AbstractEndToEndTest() {

    @Test
    fun `Replay av en inntektsmelding som feiler`() {
        val im1Mottatt = LocalDateTime.now().minusDays(1)
        val im2Mottatt = im1Mottatt.plusHours(1)

        val im1 = håndterInntektsmelding(listOf(17.januar til 1.februar), mottatt = im1Mottatt)
        håndterSøknad(17.januar til 31.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTrue(im1 in observatør.inntektsmeldingIkkeHåndtert)

        val im2 = håndterInntektsmelding(listOf(1.januar til 16.januar), mottatt = im2Mottatt)
        assertEquals(januar, inspektør.periode(1.vedtaksperiode))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTrue(im1 in observatør.inntektsmeldingIkkeHåndtert)
        assertTrue(im2 to 1.vedtaksperiode.id(a1) in observatør.inntektsmeldingHåndtert)

        assertEquals(
            "forventer at vedtaksperioden er åpen for endring når inntekt håndteres (tilstand Avsluttet)",
            assertThrows<IllegalStateException> { håndterSøknad(februar) }.message
        )

        assertVarsel(Varselkode.RV_IM_3, 2.vedtaksperiode.filter())
    }

    @Test
    fun `Inntektsmelding med begrunnelseForReduksjon & FF kommer før kort søknad`() {
        håndterInntektsmelding(emptyList(), begrunnelseForReduksjonEllerIkkeUtbetalt = "VilIkke", førsteFraværsdag = 1.januar)
        nyPeriode(1.januar til 16.januar)
        assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertEquals(listOf(1.januar til 1.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Inntektsmelding med begrunnelseForReduksjon & AGP kommer før kort søknad`() {
        håndterInntektsmelding(listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "VilIkke")
        nyPeriode(1.januar til 16.januar)
        assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertEquals(listOf(1.januar til 16.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Avhengig av replay av inntektsmelding for inntekt også i ikke-ghost-situasjon - første fraværsdag kant-i-kant`() {
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            førsteFraværsdag = 21.januar
        )
        assertEquals(1, observatør.inntektsmeldingIkkeHåndtert.size)
        assertEquals(listOf(21.januar), inspektør.inntektInspektør.inntektsdatoer)
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Avhengig av replay av inntektsmelding for inntekt også i ikke-ghost-situasjon - gap til første fraværsdag`() {
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            førsteFraværsdag = 25.januar
        )
        assertEquals(1, observatør.inntektsmeldingIkkeHåndtert.size)
        assertEquals(listOf(25.januar), inspektør.inntektInspektør.inntektsdatoer)
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Når arbeidsgiver bommer med første fraværsdag, og IM kommer før søknad er vi avhengig av replay-sjekk mot første fraværsdag for å gå videre når søknaden kommer`() {
        nyttVedtak(januar)
        assertEquals(listOf(1.januar), inspektør.inntektInspektør.inntektsdatoer)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            førsteFraværsdag = 13.februar
        )
        assertEquals(1, observatør.inntektsmeldingIkkeHåndtert.size)
        assertEquals(listOf(13.februar, 1.januar), inspektør.inntektInspektør.inntektsdatoer)
        håndterSøknad(Sykdom(12.februar, 28.februar, 100.prosent))

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `replay av IM medfører ikke at allerede revurdert skjæringstidspunkt revurderes på nytt`() {
        nyttVedtak(mars)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            beregnetInntekt = INNTEKT + 500.daglig
        )

        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        håndterSkjønnsmessigFastsettelse(1.mars, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT + 500.daglig)))
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        nullstillTilstandsendringer()
        nyPeriode(januar)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, TilstandType.AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar)
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)

        håndterYtelser(1.vedtaksperiode)
        // ingen simulering fordi det er ingen endring
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertEquals(4, inspektør.vilkårsgrunnlagHistorikkInnslag().size) // fra førstegangsbehandling, korrigert IM, skjønnsmessig fastsatt & periode out-of-order på nytt skjæringstidspunkt
    }
}
