package no.nav.helse.spleis.e2e

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.ForkasteAuu::class)
internal class ForkasteAuuTest: AbstractDslTest() {

    @Test
    fun `En ensom auu`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `En auu med mindre en 17 dager til neste periode forkastes ikke - forkaster heller ikke perioder bak`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(3.februar til 28.februar)
            nullstillTilstandsendringer()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertInfo("Kan ikke etterkomme anmodning om forkasting", 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `En auu med 18 dager til neste periode forkastes`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(4.februar til 28.februar)
            nullstillTilstandsendringer()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }
}