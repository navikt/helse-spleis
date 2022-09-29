package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.Varselkode
import no.nav.helse.person.Varselkode.RV_OO_1
import no.nav.helse.person.Varselkode.RV_OO_2
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurderOutOfOrder::class)
internal class RevurderingFerieTest : AbstractEndToEndTest() {
    @Test
    fun `Periode med bare ferie, så kommer en tidligere periode med sykdom - ferie skal revurderes`() = Toggle.FerieTilAvsluttetUtenUtbetaling.disable {
        håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(5.februar, 28.februar))
        håndterInntektsmelding(listOf(5.februar til 21.februar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyttVedtak(1.januar, 31.januar)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `Periode med bare ferie, så kommer en tidligere periode med sykdom - ferie skal ikke revurderes`() = Toggle.FerieTilAvsluttetUtenUtbetaling.enable {
        håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(5.februar, 28.februar))
        håndterInntektsmelding(listOf(5.februar til 21.februar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyttVedtak(1.januar, 31.januar)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertIngenVarsel(RV_OO_2, 1.vedtaksperiode.filter())
        assertVarsel(RV_OO_1, 2.vedtaksperiode.filter())
    }

    @Test
    fun `Forlengelse med bare ferie, så kommer en tidligere periode med sykdom - ferie skal ikke revurderes`() = Toggle.FerieTilAvsluttetUtenUtbetaling.enable {
        nyttVedtak(5.februar, 28.februar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), Søknad.Søknadsperiode.Ferie(1.mars, 31.mars))
        håndterInntektsmelding(listOf(5.mars til 21.mars))
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyttVedtak(1.januar, 31.januar)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        assertVarsel(RV_OO_2, 1.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_2, 2.vedtaksperiode.filter())
        assertVarsel(RV_OO_1, 3.vedtaksperiode.filter())
    }

    @Test
    fun `Syk - Ferie - Syk, ferie skal i AUU og ikke revurderes ved revurdering av første periode`() = Toggle.FerieTilAvsluttetUtenUtbetaling.enable {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 28.februar))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        forlengVedtak(1.mars, 31.mars)

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(3.vedtaksperiode, AVSLUTTET)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Sykedag, 90)))

        assertTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(3.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `Syk - Ferie - Syk, ferie skal i Avsluttet og revurderes ved revurdering av første periode`() = Toggle.FerieTilAvsluttetUtenUtbetaling.disable {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        forlengVedtak(1.mars, 31.mars)

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVSLUTTET)
        assertTilstand(3.vedtaksperiode, AVSLUTTET)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Sykedag, 90)))

        assertTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstand(2.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVSLUTTET)
        assertTilstand(3.vedtaksperiode, AVSLUTTET)
    }
}