package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class VarselOmFlereInntektsmeldingerTest : AbstractEndToEndTest() {

    @Test
    @Disabled("Test for feilaktig varsel https://trello.com/c/kRMN47Ly")
    fun `Prodbug - Feilaktig varsel om flere inntektsmeldinger`() {
        håndterSykmelding(Sykmeldingsperiode(22.mars(2021), 28.mars(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(22.mars(2021), 28.mars(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(29.mars(2021), 5.april(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(29.mars(2021), 5.april(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(29.mars(2021), 5.april(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(6.april(2021), 16.april(2021), 50.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(22.mars(2021) til 6.april(2021)), førsteFraværsdag = 22.mars(2021))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(6.april(2021), 16.april(2021), 50.prosent))

        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(vedtaksperiodeId = 3.vedtaksperiode, inntekt = INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2020) til 1.april(2021) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(17.april(2021), 30.april(2021), 20.prosent))
        assertTrue(inspektør.personLogg.warn().none{ w ->
            w.toString().contains("Mottatt flere inntektsmeldinger")
        })
    }
}
