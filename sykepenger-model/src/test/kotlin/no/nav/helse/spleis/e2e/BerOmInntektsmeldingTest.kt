package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class BerOmInntektsmeldingTest : AbstractEndToEndTest() {

    @Test
    fun `Ber om inntektsmelding når vi ankommer AVVENTER_INNTEKTSMELDING_FERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        assertNoErrors(inspektør)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Ber om inntektsmelding når vi ankommer AVVENTER_INNTEKTSMELDING_UFERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))

        assertNoErrors(inspektør)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(
            2.vedtaksperiode,
            START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP
        )
        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Ber ikke om inntektsmelding når vi ankommer AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 28.februar, 100.prosent))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))

        assertNoErrors(inspektør)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(
            2.vedtaksperiode,
            START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertEquals(0, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `vedtaksperiode uten søknad som går til infotrygd ber ikke om inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(21.januar, 28.februar, 100.prosent))
        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)

        assertEquals(0, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Sender ut event om at vi ikke trenger inntektsmelding når vi forlater AVVENTER_INNTEKTSMELDING_FERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )

        assertEquals(1, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Sender ut event om at vi ikke trenger inntektsmelding når vi forlater AVVENTER_INNTEKTSMELDING_UFERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(2.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 2.februar)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP
        )

        assertEquals(1, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Sender ikke ut ManglendeInntektsmeldingEvent hvis vi har en tilstøtende forkastet sykeperiode - AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 1.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 25.februar, 100.prosent))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.februar, 25.februar, 100.prosent))

        assertFalse(3.vedtaksperiode(ORGNUMMER) in observatør.manglendeInntektsmeldingVedtaksperioder)

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }
}
