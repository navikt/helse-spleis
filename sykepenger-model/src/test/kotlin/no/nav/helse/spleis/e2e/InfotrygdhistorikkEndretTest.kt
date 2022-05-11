package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import no.nav.helse.assertForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class InfotrygdhistorikkEndretTest: AbstractEndToEndTest() {
    private val gammelHistorikk = LocalDateTime.now().minusHours(24)
    private val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  31.januar, 100.prosent, INNTEKT))
    private val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))

    @Test
    fun `infotrygdhistorikken var tom`() {
        periodeTilGodkjenning()
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `cachet infotrygdhistorikk på periode grunnet påminnelse på annen vedtaksperiode, skal fortsatt reberegne`() {
        periodeTilGodkjenning()
        håndterSykmelding(Sykmeldingsperiode(1.mai, 31.mai, 100.prosent))
        håndterSøknad(Sykdom(fom = 1.mai, tom = 31.mai, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertForventetFeil(
            forklaring = "Denne testen skal funke når vi har vanlig påminnelseslogikk igjen",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            }
        )

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
        assertForventetFeil(
            forklaring = "Denne testen skal funke når vi har vanlig påminnelseslogikk igjen",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            }
        )
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
