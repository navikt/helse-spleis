package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_1
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_GODKJENT
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.UTBETALT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FjerneGodkjenningsbehovTest: AbstractDslTest() {

    @Test
    fun `mottak av vedtak fattet fungerer på samme måte som godkjenningsbehov med tommel opp`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            håndterVedtakFattet(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, TIL_UTBETALING)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertEquals(UTBETALT, inspektør.utbetalingtilstand(0))
        }
    }

    @Test
    fun `mottak av kan ikke behandles her fungerer på samme måte som godkjenningsbehov med tommel ned`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            nullstillTilstandsendringer()
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING)
            håndterKanIkkeBehandlesHer(1.vedtaksperiode)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
            assertEquals(IKKE_GODKJENT, inspektør.utbetalingtilstand(0))
        }
    }

    @Test
    fun `mottak av vedtak fattet fungerer på samme måte som godkjenningsbehov med tommel opp ved revurdering`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterVedtakFattet(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(UTBETALT, inspektør.utbetalingtilstand(0))
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1, forklaring = "forklaring")))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            håndterVedtakFattet(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, TIL_UTBETALING)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertEquals(UTBETALT, inspektør.utbetalingtilstand(1))
        }
    }

    @Test
    fun `mottak av kan ikke behandles her fungerer på samme måte som godkjenningsbehov med tommel ned ved automatisk behandlet revurdering`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterVedtakFattet(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(UTBETALT, inspektør.utbetalingtilstand(0))
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1, forklaring = "forklaring")))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            håndterKanIkkeBehandlesHer(1.vedtaksperiode, automatisert = true)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertEquals(IKKE_GODKJENT, inspektør.utbetalingtilstand(1))
            assertInfo("Revurderingen ble avvist automatisk - hindrer tilstandsendring for å unngå saker som blir stuck", 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `mottak av kan ikke behandles her fungerer på samme måte som godkjenningsbehov med tommel ned ved manuelt behandlet revurdering`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterVedtakFattet(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(UTBETALT, inspektør.utbetalingtilstand(0))
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1, forklaring = "forklaring")))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            håndterKanIkkeBehandlesHer(1.vedtaksperiode, automatisert = false)
            assertSisteTilstand(1.vedtaksperiode, REVURDERING_FEILET)
            assertEquals(IKKE_GODKJENT, inspektør.utbetalingtilstand(1))
            assertVarsel(RV_UT_1, 1.vedtaksperiode.filter())
        }
    }
}