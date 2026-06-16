package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AUUerSomUtbetalesIInfotrygdTest : AbstractDslTest() {


    @Test
    fun `Vi sender ut unødvendig forespørsel når en AUU utbetales i infotrygd`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(PersonUtbetalingsperiode(a1, 2.januar, 31.januar)))
            assertEquals("AVSLUTTET_UTEN_UTBETALING", observatør.overlappendeInfotrygdperioder.last().overlappendeInfotrygdperioder.single().vedtaksperiodetilstand)

            assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode })
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            håndterAnmodningOmForkasting(1.vedtaksperiode)

        }
    }

    @Test
    fun `utbetalt i infotrygd rett før`() {
        a1 {
            nyPeriode(2.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(PersonUtbetalingsperiode(a1, 1.januar, 1.januar)))
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `utbetalt i It med 1 dags gap og ikke forkasta`() {
        a1 {
            nyPeriode(3.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(PersonUtbetalingsperiode(a1, 1.januar, 1.januar)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `perioder langt frem i tid som står til godkjenning`() {
        a1 {
            nyPeriode(1.januar til 5.januar)
            nyPeriode(6.januar til 16.januar)
            tilGodkjenning(juni)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_GODKJENNING)

            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(PersonUtbetalingsperiode(a1, 1.januar, 1.januar)))

            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteForkastetTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
        }
    }
}
