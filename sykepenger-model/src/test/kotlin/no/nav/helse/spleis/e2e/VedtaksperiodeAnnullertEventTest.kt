package no.nav.helse.spleis.e2e

import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VedtaksperiodeAnnullertEventTest: AbstractEndToEndTest() {

    @Test
    fun `vi sender vedtaksperiode annullert-hendelser når saksbehandler annullerer en vedtaksperiode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterAnnullerUtbetaling()

        assertTrue(observatør.vedtaksperiodeAnnullertEventer.isNotEmpty())
    }
}