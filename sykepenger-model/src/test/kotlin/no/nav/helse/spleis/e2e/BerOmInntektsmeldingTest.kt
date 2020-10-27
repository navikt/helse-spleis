package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BerOmInntektsmeldingTest : AbstractEndToEndTest() {


    @Test
    fun `Ber om inntektsmelding når vi ankommer AVVENTER_INNTEKTSMELDING_FERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100))
        håndterYtelser(1.vedtaksperiode)

        assertNoErrors(inspektør)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_INNTEKTSMELDING_FERDIG_GAP
        )
        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Ber om inntektsmelding når vi ankommer AVVENTER_INNTEKTSMELDING_UFERDIG_GAP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100))

        assertNoErrors(inspektør)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode,
            START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP
        )
        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Ber om inntektsmelding når vi ankommer AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 28.februar, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100))

        assertNoErrors(inspektør)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(
            2.vedtaksperiode,
            START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `vedtaksperiode med søknad som går til infotrygd ber om inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(21.januar, 28.februar, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100))
        håndterYtelser(1.vedtaksperiode, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(18.januar, 21.januar, 15000, 100, ORGNUMMER)) // -> TIL_INFOTRYGD

        assertEquals(1, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `vedtaksperiode uten søknad som går til infotrygd ber ikke om inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(21.januar, 28.februar, 100))
        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)

        assertEquals(0, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }
}
