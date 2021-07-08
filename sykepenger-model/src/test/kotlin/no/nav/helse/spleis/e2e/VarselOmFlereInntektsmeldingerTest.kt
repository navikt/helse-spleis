package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VarselOmFlereInntektsmeldingerTest : AbstractEndToEndTest() {

    @Test
    fun `Prodbug - Feilaktig varsel om flere inntektsmeldinger`() {
        håndterSykmelding(Sykmeldingsperiode(22.mars(2021), 28.mars(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(22.mars(2021), 28.mars(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(29.mars(2021), 5.april(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(29.mars(2021), 5.april(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(29.mars(2021), 5.april(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(6.april(2021), 16.april(2021), 50.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(22.mars(2021) til 6.april(2021)), førsteFraværsdag = 22.mars(2021))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(6.april(2021), 16.april(2021), 50.prosent))

        håndterUtbetalingsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(
            vedtaksperiodeId = 3.vedtaksperiode, inntekt = INNTEKT, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.april(2021) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                })
        )
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(17.april(2021), 30.april(2021), 20.prosent))
        assertTrue(inspektør.personLogg.warn().none { w ->
            w.toString().contains("Mottatt flere inntektsmeldinger")
        })
    }

    @Test
    fun `Varsel om flere inntektsmeldinger hvis vi forlenger en avsluttet periode uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        )
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars, 50.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars)

        assertTrue(inspektør.personLogg.warn().any { w ->
            w.toString().contains("Mottatt flere inntektsmeldinger")
        })
    }

    @Test
    fun `Varsel om flere inntektsmeldinger hvis vi forlenger en avsluttet periode med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.februar til 16.februar), førsteFraværsdag = 1.februar)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(vedtaksperiodeId = 1.vedtaksperiode, inntekt = INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.februar(2017) til 1.januar inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars, 50.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars)

        assertTrue(inspektør.personLogg.warn().any { w ->
            w.toString().contains("Mottatt flere inntektsmeldinger")
        })
    }

}
