package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_UFERDIG
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeFlereAGTest : AbstractEndToEndTest() {
    @Test
    fun `kan ikke overstyre én AG hvis en annen AG har blitt godkjent`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a2)
        assertError("Kan ikke overstyre en pågående behandling der én eller flere perioder er behandlet ferdig")
    }

    @Test
    fun `overstyre en eldre periode hos en arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        tilGodkjenning(1.oktober, 30.oktober, a2)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_UFERDIG, a2)
    }

    @Test
    fun `overstyre og utbetalte en eldre periode hos en arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)

        tilGodkjenning(1.oktober, 30.oktober, a2)

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, a2)
    }
}
