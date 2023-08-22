package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSkjønnsmessigFastsettelse
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.inntektsmelding
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ReplayInntektsmeldingE2ETest : AbstractEndToEndTest() {

    @Test
    fun `Avhengig av replay av inntektsmelding for inntekt også i ikke-ghost-situasjon - første fraværsdag kant-i-kant`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 20.januar, 100.prosent))
        val inntektsmelding = inntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), førsteFraværsdag = 21.januar)
        håndterInntektsmelding(inntektsmelding)
        assertEquals(listOf(21.januar), inspektør.inntektInspektør.innteksdatoer) // Lagrer alltid på oppgitt innteksdato
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.januar, 31.januar, 100.prosent))
        assertTrue(inntektsmelding.aktuellForReplay(1.januar til 31.januar))
        assertEquals(listOf(1.januar, 21.januar), inspektør.inntektInspektør.innteksdatoer) // Replayer IM. Nå som personen er syk 21.januar lagres den både på 21.januar og alternativ inntektsdato (1.januar)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Avhengig av replay av inntektsmelding for inntekt også i ikke-ghost-situasjon - gap til første fraværsdag`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 20.januar, 100.prosent))
        val inntektsmelding = inntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), førsteFraværsdag = 25.januar)
        håndterInntektsmelding(inntektsmelding)
        assertEquals(listOf(25.januar), inspektør.inntektInspektør.innteksdatoer) // lagrer alltid på inntektsdato i IM
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(25.januar, 31.januar, 100.prosent))
        assertFalse(inntektsmelding.aktuellForReplay(25.januar til 31.januar))
        assertEquals(listOf(25.januar), inspektør.inntektInspektør.innteksdatoer) // Ingenting er blitt replayet
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `replay av IM medfører ikke at allerede revurdert skjæringstidspunkt revurderes på nytt`() = Toggle.AltAvTjuefemprosentAvvikssaker.enable {
        nyttVedtak(1.mars, 31.mars)
        håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = INNTEKT + 500.daglig)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING)
        håndterSkjønnsmessigFastsettelse(1.mars, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT + 500.daglig)))
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        nullstillTilstandsendringer()
        nyPeriode(1.januar til 31.januar)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, TilstandType.AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
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
