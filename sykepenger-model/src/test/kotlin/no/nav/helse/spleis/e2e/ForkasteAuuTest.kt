package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class ForkasteAuuTest : AbstractDslTest() {

    @Test
    fun `En auu vegg i vegg til neste periode forkastes`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(17.januar til 31.januar)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `En auu nesten vegg i vegg til neste periode forkastes også`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(18.januar til 31.januar)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
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
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Forkastet AUU skal forkaste sine forlengelser`() {
        a1 {
            nyPeriode(2.januar til 16.januar)
            nyPeriode(17.januar til 31.januar)
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Forkastet AUU skal ikke forkaste tidligere`() {
        a1 {
            nyPeriode(2.januar til 16.januar)
            nyPeriode(17.januar til 31.januar)
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }
}
