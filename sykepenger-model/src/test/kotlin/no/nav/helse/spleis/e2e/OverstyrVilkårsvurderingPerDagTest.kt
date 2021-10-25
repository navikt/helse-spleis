package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OverstyrVilkårsvurderingPerDagTest : AbstractEndToEndTest() {

    @Test
    fun `saksbehandler legger til avviste utenlandsdager`() {
        nyttVedtak(1.januar, 31.januar)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(22.januar, Dagtype.Avslått)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val linjer = inspektør.utbetalingslinjer(1).linjerUtenOpphør()
        assertEquals(2, linjer.size)
        assertEquals(21.januar, linjer[0].sistedato())
        assertEquals(23.januar, linjer[1].førstedato())
    }
}
