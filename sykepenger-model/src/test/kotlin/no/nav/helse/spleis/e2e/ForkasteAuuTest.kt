package no.nav.helse.spleis.e2e

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
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
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `Gjentatte Auuer etter hverandre`() {
        a1 {
            val agp = listOf(
                1.januar til 5.januar,
                8.januar til 12.januar,
                15.januar til 19.januar,
                22.januar til 22.januar
            )
            agp.forEach(::nyPeriode)
            håndterInntektsmelding(agp)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            håndterAnmodningOmForkasting(3.vedtaksperiode)
            håndterAnmodningOmForkasting(4.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Forkaster ikke AUU med etterfølgende utbetalt periode ved out of order søknad`() {
        a1 {
            håndterSøknad(Sykdom(1.februar, 16.februar, 100.prosent))
            håndterSøknad(Sykdom(17.februar, 28.februar, 100.prosent))
            håndterInntektsmelding(listOf(1.februar til 16.februar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 25.januar, 100.prosent), utenlandskSykmelding = true)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }
}