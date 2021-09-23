package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.januar
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
        håndterOverstyring((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = AG2)
        assertErrorTekst(inspektør(AG2), "Kan ikke overstyre en pågående behandling der én eller flere perioder er behandlet ferdig")
    }
}
