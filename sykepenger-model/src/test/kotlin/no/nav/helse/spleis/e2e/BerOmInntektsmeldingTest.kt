package no.nav.helse.spleis.e2e

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.START
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class BerOmInntektsmeldingTest : AbstractEndToEndTest() {

    @Test
    fun `Ber om inntektsmelding når vi ankommer AVVENTER_INNTEKTSMELDING_FERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        assertNoErrors()
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
        )
        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Sender ut event om at vi ikke trenger inntektsmelding når vi forlater AVVENTER_INNTEKTSMELDING_FERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK
        )

        assertEquals(1, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Sender ut event om at vi ikke trenger inntektsmelding når vi forlater AVVENTER_INNTEKTSMELDING_UFERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 2.februar)

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
        )

        assertEquals(1, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Sender ikke ut ManglendeInntektsmeldingEvent hvis vi har en tilstøtende forkastet sykeperiode - AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 1.februar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 1.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 25.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 25.februar, 100.prosent))

        assertFalse(3.vedtaksperiode.id(ORGNUMMER) in observatør.manglendeInntektsmeldingVedtaksperioder)

        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
        )
    }
}
