package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Test

internal class ReplayHendelserTest : AbstractEndToEndTest() {

    @Test
    fun `Håndterer vi gap?`() {
        val opprinnelig = håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        replaySykmelding(opprinnelig)
    }

    @Test
    fun `ny, tidligere sykmelding medfører umiddelbar replay av etterfølgende perioder som ikke er avsluttet eller til utbetaling`() {
        val opprinnelig = håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 27.februar, 100))
        replaySykmelding(opprinnelig)

        assertReplayAv(1.vedtaksperiode)
        assertAntallReplays(1)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, TilstandType.START, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, TilstandType.START, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `ny, tidligere sykmelding medfører replay av etterfølgende perioder som er avsluttet eller til utbetaling først når ny periode er utbetalt`() {
        val sykmeldingId = håndterSykmelding(Sykmeldingsperiode(28.januar, 28.februar, 100))
        val søknadId = håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.januar, 28.februar, 100))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 28.januar)

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        replaySykmelding(sykmeldingId)
        replaySøknad(søknadId)
        replayInntektsmelding(inntektsmeldingId)

        assertAntallReplays(1)
        assertReplayAv(1.vedtaksperiode)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 21.januar, 100))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT)
        håndterYtelser(3.vedtaksperiode)   // No history
    }

    @Test
    fun `ny sykmelding for tidligere periode håndteres`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        assertForkastetPeriodeTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP
        )
        assertTilstander(1.vedtaksperiode, TilstandType.START, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `Replay med gap hvor første periode er utbetalt skal opprette ny periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.februar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 30.januar,100)) // Initierer replay av tidligere periode
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 30.januar, 100))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
    }
}
