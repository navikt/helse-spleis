package no.nav.helse.spleis.e2e.behandlinger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BeregningIdTest: AbstractDslTest() {
    @Test
    fun `beholder samme beregningId helt frem til ny beregning av utbetalingstidslinje`() {
        a1 {
            tilGodkjenning(mars)
            assertEquals(7, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.single().endringer.size)

            val gjeldendeBeregningId = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.single().endringer.last().beregningId
            inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.single().endringer.forEach { endring ->
                assertEquals(gjeldendeBeregningId, endring.beregningId)
            }

            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            håndterYtelser(1.vedtaksperiode)

            assertEquals(10, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.single().endringer.size)

            val gjeldendeBeregningIdEtterITendring = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.single().endringer.last().beregningId
            inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.single().endringer.drop(7).forEach { endring ->
                assertEquals(gjeldendeBeregningIdEtterITendring, endring.beregningId)
            }
        }
    }
}
