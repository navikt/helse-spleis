package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RevurderKorrigertSoknadTest : AbstractEndToEndTest() {

    @Test
    fun `Korrigert søknad ved avsluttet periode - skal sette i gang en revurdering` () = Toggle.RevurderKorrigertSoknad.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(17.januar, 18.januar))
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        håndterYtelser(1.vedtaksperiode)
        assertTrue(inspektør.sykdomstidslinje[17.januar] is Dag.Feriedag)
        assertTrue(inspektør.sykdomstidslinje[18.januar] is Dag.Feriedag)

        assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar] is Fridag)
        assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[18.januar] is Fridag)

    }

}