package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class RevurderingFerieTest : AbstractEndToEndTest() {

    @Test
    fun `Periode med bare ferie, så kommer en tidligere periode med sykdom - ferie revurderes fordi senere periode sender IM med dager som overlapper`() {
        håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(5.februar, 28.februar))
        håndterInntektsmelding(listOf(5.februar til 20.februar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderingFerieTest.håndterYtelser(1.vedtaksperiode)
        this@RevurderingFerieTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        nullstillTilstandsendringer()
        nyttVedtak(1.januar til 17.januar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `Forlengelse med bare ferie, så kommer en tidligere periode med sykdom - ferie skal revurderes`() {
        nyttVedtak(5.februar til 28.februar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), Søknad.Søknadsperiode.Ferie(1.mars, 31.mars))
        håndterInntektsmelding(listOf(5.mars til 20.mars))

        assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())

        this@RevurderingFerieTest.håndterYtelser(2.vedtaksperiode)
        this@RevurderingFerieTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        nyttVedtak(1.januar til 17.januar, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `Syk - Ferie - Syk, ferie skal i Avsluttet og revurderes ved revurdering av første periode`() {
        nyttVedtak(januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 28.februar))
        this@RevurderingFerieTest.håndterYtelser(2.vedtaksperiode)
        this@RevurderingFerieTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        forlengVedtak(mars)

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVSLUTTET)
        assertTilstand(3.vedtaksperiode, AVSLUTTET)

        this@RevurderingFerieTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Sykedag, 90)))

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

        this@RevurderingFerieTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingFerieTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        this@RevurderingFerieTest.håndterYtelser(2.vedtaksperiode)
        this@RevurderingFerieTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        this@RevurderingFerieTest.håndterYtelser(3.vedtaksperiode)
        this@RevurderingFerieTest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVSLUTTET)
        assertTilstand(3.vedtaksperiode, AVSLUTTET)
    }
}
