package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InntektUnder16DagersGapTest : AbstractEndToEndTest() {

    @BeforeEach
    fun setup() {
        Toggles.PraksisendringEnabled.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggles.PraksisendringEnabled.pop()
    }

    @Test
    fun `Krever ikke ny inntektsmelding ved gap mindre enn 16 dager - ikke helg i starten eller slutten av gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(6.februar, 16.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(6.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(INNTEKT, 6.februar, it)
        }
    }

    @Test
    fun `Krever ikke ny inntektsmelding ved gap mindre enn 16 dager - syk helg i starten av gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(5.februar, 16.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(INNTEKT, 5.februar, it)
        }

    }

    @Test
    fun `Krever ikke ny inntektsmelding ved gap mindre enn 16 dager - ukjent helg i starten av gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 19.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 19.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(5.februar, 16.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(INNTEKT, 5.februar, it)
        }
    }


    @Test
    fun `Krever ny inntektsmelding ved gap på nøyaktig 16 dager - frisk helg i starten og slutten av gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 4.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 4.februar, 100.prosent), Søknad.Søknadsperiode.Arbeid(20.januar, 4.februar))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(5.februar, 16.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(null, 5.februar, it)
        }

    }

    @Test
    fun `Krever ikke ny inntektsmelding ved gap mindre enn 16 dager - syk helg i slutten av gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 25.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(10.februar, 16.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(10.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(INNTEKT, 10.februar, it)
        }
    }

    @Test
    fun `Krever ny inntektsmelding ved gap over 16 dager - ukjent helg i slutten av gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 25.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(12.februar, 16.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(12.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(null, 12.februar, it)
        }
    }

    @Test
    fun `Krever ny inntektsmelding ved gap på nøyaktig 16 dager - ukjent helg etter perioden fra infotrygd`() {
        val historikk = Utbetalingsperiode(ORGNUMMER, 1.januar til 19.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(7.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(7.februar, 28.februar, 100.prosent))

        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )

        inspektør.also {
            assertInntektForDato(null, 1.januar, it)
            assertInntektForDato(null, 7.februar, it)
        }
    }

    @Test
    fun `Krever ikke ny inntektsmelding ved gap på mindre enn 16 dager - ukjent helg etter perioden fra infotrygd`() {
        val historikk = Utbetalingsperiode(ORGNUMMER, 1.januar til 19.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(6.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(6.februar, 28.februar, 100.prosent))

        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(INNTEKT, 6.februar, it)
        }
    }

    @Test
    fun `Krever ikke ny inntektsmelding ved gap mindre enn 16 dager - gap fra infotrygd slutter på søndag`() {
        val historikk = Utbetalingsperiode(ORGNUMMER, 1.januar til 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(6.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(6.februar, 28.februar, 100.prosent))

        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(INNTEKT, 6.februar, it)
        }
    }

    @Test
    fun `Bruker nyeste inntektsmelding ved gap mindre enn 16 dager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(6.februar, 16.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 6.februar, refusjon = Triple(null, 32000.månedlig, emptyList()))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(6.februar, 16.februar, 100.prosent))

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(32000.månedlig, 6.februar, it)
            assertEquals(29034, it.totalBeløp.last())
        }
    }

    @Test
    fun `Krever ikke ny inntektsmelding ved gap på 1 dag`() {
        val historikk = Utbetalingsperiode(ORGNUMMER, 1.januar til 14.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(16.januar, 19.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(16.januar, 19.januar, 100.prosent))

        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )

        inspektør.also {
            assertInntektForDato(INNTEKT, 1.januar, it)
            assertInntektForDato(INNTEKT, 16.januar, it)
        }
    }
}
