package no.nav.helse.spleis.e2e

import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeAvbruttE2ETest : AbstractEndToEndTest() {

    @Test
    fun `vedtaksperioder avbrytes`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        forlengPeriode(1.april, 30.april)
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode, false) // <- TIL_INFOTRYGD
        assertEquals(1, observatør.avbruttePerioder())
        assertEquals(TIL_INFOTRYGD, observatør.avbrutt(4.vedtaksperiode.id(ORGNUMMER)).gjeldendeTilstand)
    }
}
