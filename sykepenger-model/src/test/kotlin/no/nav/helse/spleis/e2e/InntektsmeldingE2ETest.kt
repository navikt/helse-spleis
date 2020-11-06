package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class InntektsmeldingE2ETest : AbstractEndToEndTest() {

    @Disabled("WIP Test for inntektsmelding med refusjonsopphold")
    @Test
    fun `inntektsmelding med refusjonsopphold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 30.januar, 100))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 30.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        // -> TODO refusjon IM kommer her

        håndterSykmelding(Sykmeldingsperiode(31.januar, 28.februar, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(31.januar, 28.februar, 100))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar, refusjon = Triple(6.februar, INNTEKT, emptyList()))

        inspektør.also {
            assertEquals(Periode(1.januar, 30.januar), it.vedtaksperioder(1.vedtaksperiode).periode())
        }
    }

}
