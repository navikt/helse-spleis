package no.nav.helse.spleis.e2e

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.AnmodeOmForkastingIAUU::class)
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
    fun `En auu med mindre en 18 dager til neste periode forkastes ikke - forkaster heller ikke perioder bak`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyttVedtak(1.februar, 28.februar, 100.prosent, arbeidsgiverperiode = listOf(1.januar til 16.januar))

            nullstillTilstandsendringer()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertInfo("Kan ikke etterkomme anmodning om forkasting", 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
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
    fun `En auu vegg i vegg til neste periode forkastes`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(17.januar til 31.januar)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `En auu nesten vegg i vegg til neste periode forkastes ikke`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(18.januar til 31.januar)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
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

    @Test
    fun `AUU 1 januar påvirker AGP for periode i mars`() {
        a1 {
            nyPeriode(1.januar til 1.januar)
            nyPeriode(16.januar til 16.januar)
            nyPeriode(1.februar til 1.februar)
            nyPeriode(15.februar til 15.februar)
            nyPeriode(1.mars til 31.mars)

            val agp = inspektør.arbeidsgiver.arbeidsgiverperiode(1.mars til 31.mars)?.toList()?.grupperSammenhengendePerioder()
            assertEquals(listOf(
                1.januar til 1.januar,
                16.januar til 16.januar,
                1.februar til 1.februar,
                15.februar til 15.februar,
                1.mars til 12.mars
            ), agp)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(5.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            nullstillTilstandsendringer()

            håndterAnmodningOmForkasting(1.vedtaksperiode)
            val nyAgp = inspektør.arbeidsgiver.arbeidsgiverperiode(1.mars til 31.mars)?.toList()?.grupperSammenhengendePerioder()

            assertForventetFeil(
                forklaring = "1.januar påvirker arbeidsgiverperioden for mars-perioden og burde ikke forkastes",
                nå = {
                    assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
                    assertEquals(listOf(
                        16.januar til 16.januar,
                        1.februar til 1.februar,
                        15.februar til 15.februar,
                        1.mars til 13.mars
                    ), nyAgp)
                },
                ønsket = {
                    assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
                    assertEquals(listOf(
                        1.januar til 1.januar,
                        16.januar til 16.januar,
                        1.februar til 1.februar,
                        15.februar til 15.februar,
                        1.mars til 12.mars
                    ), nyAgp)
                }
            )
        }
    }
}