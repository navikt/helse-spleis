package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.til
import no.nav.helse.i
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderingPåTversAvMaksdatotellingerTest : AbstractDslTest() {

    @Test
    fun `unødvendig revurdering av periode 2 fordi periode 1 ikke påvirker maksdatotellingen for periode 2`() {
        a1 {
            nyttVedtak(januar.i(2017))
            nyttVedtak(januar.i(2018))
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET)
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(
                ArbeidsgiverUtbetalingsperiode(a1, 1.desember(2016), 31.desember(2016)))
            )
            assertForventetFeil(
                nå = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING)
                    assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_REVURDERING)
                },
                ønsket = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING)
                    assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET)
                }
            )
        }
    }

    @Test
    fun `unødvendig revurdering av periode fordi IT-utbetaling ett år tidligere ikke påvirker maksdatotellingen for perioden`() {
        a1 {
            nyttVedtak(januar.i(2018))
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(
                ArbeidsgiverUtbetalingsperiode(a1, 1.desember(2016), 31.desember(2016)))
            )
            assertForventetFeil(
                nå = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
                },
                ønsket = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
                }
            )
        }
    }

    @Test
    fun `revurdering på tvers av 26 ukers gap pga forskyving av maksdato`() {
        medMaksSykedager(15) // NB: Setter maksdato til 15 dager for å lettere kunne teste
        a1 {
            nyttVedtak(januar.i(2015)) // Bare for å få inn personen

            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(
                ArbeidsgiverUtbetalingsperiode(a1, 1.desember(2017), 11.desember(2017)))
            )
            nyttVedtak(januar)

            assertEquals(3, inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.avvistDagTeller)

            nyttVedtak(13.juli til 17.august) // 11.juli-13,14(lør),15(søn).juli kan for testen funke som f.o.m., siden vi har 3 avviste dager på slutten av forrige(?)

            assertEquals(0, inspektør.utbetalingstidslinjer(3.vedtaksperiode).inspektør.avvistDagTeller)

            // Fjerner infotrygdutbetalinger for 2.-11.desember, gjør at siste dager på 2.vedtaksperiode blir utbetalt allikevel, og utbetalingsgapet til 3.vedtaksperiode blir ikke lenger over 26 uker
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(
                ArbeidsgiverUtbetalingsperiode(a1, 1.desember(2017), 1.desember(2017)))
            )

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(3.vedtaksperiode)

            assertEquals(12, inspektør.utbetalingstidslinjer(3.vedtaksperiode).inspektør.avvistDagTeller)
            assertVarsler(3.vedtaksperiode, Varselkode.RV_UT_23)
        }
    }

}
