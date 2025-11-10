package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AnnullereTidligereUtbetalingE2ETest : AbstractDslTest() {

    @Test
    fun `annullere tidligere utbetaling på samme arbeidsgiver`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(mars)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsel(Varselkode.RV_RV_7, 2.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `revurdering mens annullere tidligere utbetaling`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(mars)
            håndterAnnullering(1.vedtaksperiode)
            val err = assertThrows<IllegalStateException> {
                håndterSøknad(januar)
            }
            assertEquals("Kan ikke håndtere søknad mens perioden er i TilAnnullering", err.message)
        }
    }

    @Test
    fun `revurdering mens annullere senere utbetaling`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(mars)
            håndterAnnullering(2.vedtaksperiode)
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_ANNULLERING)
        }
    }

    @Test
    fun `annullere tidligere utbetaling på annen arbeidsgiver`() {
        a1 {
            nyttVedtak(januar)
        }
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            assertIngenFunksjonelleFeil()
            assertEquals(Utbetalingtype.ANNULLERING, inspektør.sisteUtbetaling().type)
        }
        a2 {
            assertVarsel(Varselkode.RV_RV_7, 1.vedtaksperiode.filter())
        }
    }
}
