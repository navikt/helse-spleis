package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.manuellFeriedag
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeFlereAGTest : AbstractDslTest() {

    @Test
    fun `kan ikke overstyre én AG hvis en annen AG har blitt godkjent`() {
        a1 {
            nyPeriode(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            nyPeriode(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) })
            assertIngenFunksjonelleFeil()
        }
        nullstillTilstandsendringer()
        a1 { assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING) }
        a2 { assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `overstyre en eldre periode hos en arbeidsgiver`() {
        a1 { nyttVedtak(januar) }
        a2 { tilGodkjenning(1.oktober til 30.oktober) }
        a1 {
            håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) })
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `overstyre og utbetalte en eldre periode hos en arbeidsgiver`() {
        a1 { nyttVedtak(januar) }
        a2 { tilGodkjenning(1.oktober til 30.oktober) }

        a1 {
            håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) })
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
    }
}
