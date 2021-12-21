package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mai
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class InfotrygdhistorikkEndretTest: AbstractEndToEndTest() {
    private val gammelHistorikk = LocalDateTime.now().minusHours(24)
    private val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar,  31.januar, 100.prosent, INNTEKT))
    private val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.januar, INNTEKT, true))

    @Test
    fun `infotrygdhistorikken var tom`() {
        periodeTilGodkjenning()
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertTrue(observatør.reberegnedeVedtaksperioder.contains(1.vedtaksperiode(ORGNUMMER)))
    }

    @Test
    fun `cachet infotrygdhistorikk på relevant periode grunnet påminnelse på annen vedtaksperiode, skal fortsatt reberegne`() {
        periodeTilGodkjenning()
        håndterSykmelding(Sykmeldingsperiode(1.mai, 31.mai, 100.prosent))
        håndterPåminnelse(2.vedtaksperiode, MOTTATT_SYKMELDING_UFERDIG_GAP)
        håndterUtbetalingshistorikk(2.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertTrue(observatør.reberegnedeVedtaksperioder.contains(1.vedtaksperiode(ORGNUMMER)))
    }

    @Test
    fun `infotrygdhistorikken blir tom`() {
        periodeTilGodkjenning(utbetalinger, inntektshistorikk)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertTrue(observatør.reberegnedeVedtaksperioder.contains(1.vedtaksperiode(ORGNUMMER)))
    }

    @Test
    fun `infotrygdhistorikken er uendret`() {
        periodeTilGodkjenning(utbetalinger, inntektshistorikk)
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }

    private fun periodeTilGodkjenning(perioder: List<Infotrygdperiode> = emptyList(), inntektsopplysning: List<Inntektsopplysning> = emptyList()) {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(1.vedtaksperiode, *perioder.toTypedArray(), inntektshistorikk = inntektsopplysning, besvart = gammelHistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, *perioder.toTypedArray(), inntektshistorikk = inntektsopplysning, besvart = gammelHistorikk)
        håndterSimulering(1.vedtaksperiode)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }
}
