package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class InfotrygdhistorikkEndretTest: AbstractEndToEndTest() {
    private val gammelHistorikk = LocalDateTime.now().minusHours(24)
    private val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  31.januar, 100.prosent, INNTEKT))
    private val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))

    @BeforeEach
    fun setup() {
        Toggles.RebregnUtbetalingVedHistorikkendring.enable()
    }

    @AfterEach
    fun teardown() {
        Toggles.RebregnUtbetalingVedHistorikkendring.pop()
    }

    @Test
    fun `infotrygdhistorikken var tom`() {
        periodeTilGodkjenning()
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `infotrygdhistorikken blir tom`() {
        periodeTilGodkjenning(utbetalinger, inntektshistorikk)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `infotrygdhistorikken er uendret`() {
        periodeTilGodkjenning(utbetalinger, inntektshistorikk)
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }

    private fun periodeTilGodkjenning(perioder: List<Infotrygdperiode> = emptyList(), inntektsopplysning: List<Inntektsopplysning> = emptyList()) {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(1.vedtaksperiode, *perioder.toTypedArray(), inntektshistorikk = inntektsopplysning, besvart = gammelHistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, *perioder.toTypedArray(), inntektshistorikk = inntektsopplysning, besvart = gammelHistorikk)
        håndterSimulering(1.vedtaksperiode)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }
}
