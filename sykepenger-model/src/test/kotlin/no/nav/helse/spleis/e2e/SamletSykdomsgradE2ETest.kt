package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.UtbetalingstidslinjeInspektør
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Utbetaling.GodkjentUtenUtbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Sendt
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class SamletSykdomsgradE2ETest: AbstractEndToEndTest() {

    @Test
    fun `avviser dager under 20 prosent`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 19.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 20.januar, 19.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(0))
        val utbetalingstidslinje = inspektør.utbetalingUtbetalingstidslinje(0)
        assertTrue(utbetalingstidslinje[17.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertTrue(utbetalingstidslinje[18.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertTrue(utbetalingstidslinje[19.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        UtbetalingstidslinjeInspektør(utbetalingstidslinje).also {
            assertEquals(3, it.avvistDagTeller)
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
    }

    @Test
    fun `avviser dager under 20 prosent på forlengelser`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 19.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 20.januar, 19.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.januar, 31.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        val utbetalingstidslinje = inspektør.utbetalingUtbetalingstidslinje(1)
        assertTrue(utbetalingstidslinje[17.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertTrue(utbetalingstidslinje[18.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertTrue(utbetalingstidslinje[19.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        UtbetalingstidslinjeInspektør(utbetalingstidslinje).also {
            assertEquals(3, it.avvistDagTeller)
        }
        assertEquals(Sendt, inspektør.utbetalingtilstand(1))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }

    @Disabled("når vi mottar korrigert søknad ligger det igjen warnings fra før som ikke lengre gjelder")
    @Test
    fun `opprinnelig søknad med 100 prosent arbeidshelse blir korrigert slik at sykdomsgraden blir 100 prosent `() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 20.januar, 100.prosent, 100.prosent)) // 100 prosent arbeidshelse => 0 prosent syk
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 20.januar, 100.prosent)) // korrigert søknad med 0 prosent arbeidshelse => 100 prosent syk
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertNoWarnings(inspektør)
    }
}
