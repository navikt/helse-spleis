package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AnmodningOmForkastingTest : AbstractDslTest() {

    @Test
    fun `anmodning avslås av en avsluttet vedtaksperiode`() {
        a1 {
            nyttVedtak(januar)
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertInfo("Avslår anmodning om forkasting i Avsluttet", 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `når anmodning innfris forkastes alt på skjæringstidspunktet`() {
        (a1 og a2).nyeVedtak(januar)
        a1 {
            nyPeriode(mars)
        }
        a2 {
            nyPeriode(mars)
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 {
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertInfo("Kan forkastes fordi evt. overlappende utbetalinger er annullerte/forkastet", 2.vedtaksperiode.filter())
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
            nyPeriode(januar)
            nyPeriode(mars)
            nyPeriode(mai)
            nyPeriode(juli)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(2.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `anmodning innfris av en vedtaksperiode som avventer inntektsmelding`() {
        a1 {
            nyPeriode(januar)
            nullstillTilstandsendringer()
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Forkaster senere periode påvirker ikke pågående revurdering på tidligere periode med samme skjæringstidspunkt`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.januar, Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)

            håndterSøknad(mars)

            nullstillTilstandsendringer()

            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            håndterAnmodningOmForkasting(3.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `forkasting uten force påvirker ikke vedtaksperioder som ikke kan forkastes`() {
        medJSONPerson("/personer/auu-med-utbetalt-vedtak-etter.json")
        a1 {
            håndterOverstyrTidslinje((1.januar til 16.januar).map {
                ManuellOverskrivingDag(it, Dagtype.SykedagNav, 100)
            })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterAnmodningOmForkasting(1.vedtaksperiode)

            inspektør.utbetalinger(1.vedtaksperiode).also { utbetalinger ->
                assertEquals(1, utbetalinger.size)
                assertEquals(Utbetalingstatus.IKKE_UTBETALT, utbetalinger.single().status)
            }

            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `forkasting med force påvirker bare vedtaksperioden som forces`() {
        medJSONPerson("/personer/auu-med-utbetalt-vedtak-etter.json")
        a1 {
            håndterOverstyrTidslinje((1.januar til 16.januar).map {
                ManuellOverskrivingDag(it, Dagtype.SykedagNav, 100)
            })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterAnmodningOmForkasting(1.vedtaksperiode, force = true)

            inspektør.utbetalinger(1.vedtaksperiode).also { utbetalinger ->
                assertEquals(1, utbetalinger.size)
                assertEquals(Utbetalingstatus.FORKASTET, utbetalinger.single().status)
            }
            inspektør.utbetalinger(2.vedtaksperiode).also { utbetalinger ->
                assertEquals(1, utbetalinger.size)
                assertEquals(Utbetalingstatus.UTBETALT, utbetalinger.single().status)
            }

            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING)
        }
    }
}
