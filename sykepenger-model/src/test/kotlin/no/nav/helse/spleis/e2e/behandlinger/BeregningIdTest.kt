package no.nav.helse.spleis.e2e.behandlinger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.BehandlingView
import no.nav.helse.person.BehandlingendringView
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class BeregningIdTest: AbstractDslTest() {

    @Test
    fun `beholder samme beregningId helt frem til ny beregning av utbetalingstidslinje`() {
        a1 {
            tilGodkjenning(mars)
            with(inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger) {
                assertEquals(1, size)
                with(single()) {
                    assertEquals(7, endringer.size)
                    assertLikBeregningId("IKKE_UTBETALT")
                }
            }

            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            håndterYtelser(1.vedtaksperiode)

            with(inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger) {
                assertEquals(1, size)
                with(single()) {
                    assertEquals(10, endringer.size)
                    assertNotEquals(endringer[6].beregningId, endringer.last().beregningId)
                }
            }

            with(inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger) {
                assertEquals(1, size)
                with(single()) {
                    assertEquals(10, endringer.size)
                    with(endringer.drop(7)) {
                        assertLikBeregningId("IKKE_UTBETALT")
                    }
                }
            }
        }
    }

    @Test
    fun `En periode som annulleres`() {
        a1 {
            tilGodkjenning(januar)
            with(inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger) {
                assertEquals(1, size)
                with(single()) {
                    assertEquals(7, endringer.size)
                    assertLikBeregningId("IKKE_UTBETALT")
                }
            }
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            with(inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger) {
                assertEquals(1, size)
                with(single()) {
                    assertEquals(7, endringer.size)
                    assertLikBeregningId("UTBETALT")
                }
            }

            håndterAnnullering(1.vedtaksperiode)
            with(inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger) {
                assertEquals(2, size)
                with(get(1)) {
                    assertEquals(2, endringer.size)
                    assertLikBeregningId("OVERFØRT")
                }
            }
            håndterUtbetalt()
            with(inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger) {
                assertEquals(2, size)
                with(get(1)) {
                    assertEquals(2, endringer.size)
                    assertLikBeregningId("ANNULLERT")
                }
            }
        }
    }

    private fun BehandlingView.assertLikBeregningId(forventetSisteUtbetalingstatus: String? = null)  = endringer.assertLikBeregningId(forventetSisteUtbetalingstatus)

    private fun List<BehandlingendringView>.assertLikBeregningId(forventetSisteUtbetalingstatus: String? = null) {
        val første = first().beregningId
        forEach { endring -> assertEquals(første, endring.beregningId) }
        val sisteUtbetalingStatus = last().utbetaling?.inspektør?.tilstand?.name
        assertEquals(forventetSisteUtbetalingstatus, sisteUtbetalingStatus)
    }
}
