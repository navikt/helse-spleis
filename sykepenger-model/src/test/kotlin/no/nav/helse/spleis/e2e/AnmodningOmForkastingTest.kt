package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Test

internal class AnmodningOmForkastingTest: AbstractDslTest() {

    @Test
    fun `anmodning avslås av en avsluttet vedtaksperiode`(){
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertInfo("Avslår anmodning om forkasting i AVSLUTTET", 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `når anmodning innfris forkastes alt på skjæringstidspunktet`(){
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            nyPeriode(1.mars til 31.mars)
        }
        a2 {
            nyPeriode(1.mars til 31.mars)
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 {
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertInfo("Etterkommer anmodning om forkasting", 2.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `når anmodning innfris forkastes alt senere`() {
        a1 {
            nyPeriode(1.januar til 31.januar)
            nyPeriode(1.mars til 31.mars)
            nyPeriode(1.mai til 31.mai)
            nyPeriode(1.juli til 31.juli)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `anmodning innfris av en vedtaksperiode som avventer inntektsmelding`(){
        a1 {
            nyPeriode(1.januar til 31.januar)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `anmodning innfris av en vedtaksperiode som avventer historikk`(){
        a1 {
            nyPeriode(1.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Anmodning om å forkaste periode i AUU forkaster kun den éne perioden`() = Toggle.ForkasteAuu.enable {
        (a1 og a2).forEach { ag -> ag {
            nyPeriode(1.januar til 4.januar)
            nyPeriode(6.januar til 9.januar)
            nyPeriode(10.januar til 13.januar)
            nyPeriode(1.mars til 31.mars)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }}

        a1 {
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }
}