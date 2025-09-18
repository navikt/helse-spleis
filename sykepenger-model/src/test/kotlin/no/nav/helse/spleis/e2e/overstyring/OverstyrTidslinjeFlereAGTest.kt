package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeFlereAGTest : AbstractEndToEndTest() {

    @Test
    fun `kan ikke overstyre én AG hvis en annen AG har blitt godkjent`() {
        tilGodkjenning(januar, a1, a2)
        this@OverstyrTidslinjeFlereAGTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        this@OverstyrTidslinjeFlereAGTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@OverstyrTidslinjeFlereAGTest.håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a2)
        assertIngenFunksjonelleFeil()
        nullstillTilstandsendringer()
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `overstyre en eldre periode hos en arbeidsgiver`() {
        nyttVedtak(januar, orgnummer = a1)
        tilGodkjenning(1.oktober til 30.oktober, a2)
        this@OverstyrTidslinjeFlereAGTest.håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a2)
    }

    @Test
    fun `overstyre og utbetalte en eldre periode hos en arbeidsgiver`() {
        nyttVedtak(januar, orgnummer = a1)

        tilGodkjenning(1.oktober til 30.oktober, a2)

        this@OverstyrTidslinjeFlereAGTest.håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a2)

        this@OverstyrTidslinjeFlereAGTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter(orgnummer = a1))
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@OverstyrTidslinjeFlereAGTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        this@OverstyrTidslinjeFlereAGTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, a2)
    }
}
