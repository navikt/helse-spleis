package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.*
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeFlereAGTest : AbstractEndToEndTest() {

    private companion object {
        private val AG1 = "987654321"
        private val AG2 = "123456789"
    }

    @Test
    fun `kan ikke overstyre én AG hvis en annen AG har blitt godkjent`() {
        tilGodkjenning(1.januar, 31.januar, AG1, AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG2)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = AG2)
        assertErrorTekst(inspektør(AG2), "Kan ikke overstyre en pågående behandling der én eller flere perioder er behandlet ferdig")
    }

    @ForventetFeil("Overstyring på arbeidsgiver 1 skal ikke medføre at arbeidsgiver 2 sin urelaterte periode påvirkes")
    @Test
    fun `overstyre en eldre periode hos en arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = AG1)
        tilGodkjenning(1.oktober, 30.oktober, AG2)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AG1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, AG2)
    }

    @ForventetFeil("Dersom det er riktig at arbeidsgiver 2 påvirkes, skal uansett arbeidsgiver 2 få Gjennoppta behandling når arbeidsgiver 1 avsluttes")
    @Test
    fun `overstyre og utbetalte en eldre periode hos en arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = AG1)
        tilGodkjenning(1.oktober, 30.oktober, AG2)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, AG1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, AG2)
    }
}
