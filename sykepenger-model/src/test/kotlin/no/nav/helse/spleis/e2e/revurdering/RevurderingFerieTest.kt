package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyttVedtak
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
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class RevurderingFerieTest : AbstractDslTest() {

    @Test
    fun `Periode med bare ferie, så kommer en tidligere periode med sykdom - ferie revurderes fordi senere periode sender IM med dager som overlapper`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(5.februar, 28.februar))
            håndterInntektsmelding(listOf(5.februar til 20.februar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            nullstillTilstandsendringer()
            nyttVedtak(1.januar til 17.januar)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Forlengelse med bare ferie, så kommer en tidligere periode med sykdom - ferie skal revurderes`() {
        a1 {
            nyttVedtak(5.februar til 28.februar)
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), Søknad.Søknadsperiode.Ferie(1.mars, 31.mars))
            håndterInntektsmelding(listOf(5.mars til 20.mars))

            assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())

            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            nyttVedtak(1.januar til 17.januar)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Syk - Ferie - Syk, ferie skal i Avsluttet og revurderes ved revurdering av første periode`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 28.februar))
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            forlengVedtak(mars)

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            assertTilstand(3.vedtaksperiode, AVSLUTTET)

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Sykedag, 90)))

            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            håndterYtelser(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            assertTilstand(3.vedtaksperiode, AVSLUTTET)
        }
    }
}
