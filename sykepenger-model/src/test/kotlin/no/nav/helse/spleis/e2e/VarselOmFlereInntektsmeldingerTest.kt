package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VarselOmFlereInntektsmeldingerTest : AbstractEndToEndTest() {

    @Test
    fun `Prodbug - Feilaktig varsel om flere inntektsmeldinger`() {
        håndterSykmelding(Sykmeldingsperiode(22.mars(2021), 28.mars(2021), 100.prosent))
        håndterSøknad(Sykdom(22.mars(2021), 28.mars(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(29.mars(2021), 5.april(2021), 100.prosent))
        håndterSøknad(Sykdom(29.mars(2021), 5.april(2021), 100.prosent))
        håndterSøknad(Sykdom(29.mars(2021), 5.april(2021), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(6.april(2021), 16.april(2021), 50.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(22.mars(2021) til 6.april(2021)), førsteFraværsdag = 22.mars(2021))
        håndterSøknad(Sykdom(6.april(2021), 16.april(2021), 50.prosent))

        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 3.vedtaksperiode, inntekt = INNTEKT, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.mars(2020) til 1.februar(2021) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                })
        )
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(17.april(2021), 30.april(2021), 20.prosent))
        assertTrue(person.personLogg.warn().none { w ->
            w.toString().contains("Mottatt flere inntektsmeldinger")
        })
    }

    @Test
    fun `Varsel om flere inntektsmeldinger hvis vi forlenger en avsluttet periode uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars, 50.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars)

        assertNoWarning(1.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertWarning("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.", 1.vedtaksperiode.filter())
        assertNoWarning(2.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertWarning("Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første/bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt.", 2.vedtaksperiode.filter())
        assertWarning("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.", 2.vedtaksperiode.filter())
    }

    @Test
    fun `Varsel om flere inntektsmeldinger hvis vi forlenger en avsluttet periode med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.februar til 16.februar), førsteFraværsdag = 1.februar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, inntekt = INNTEKT, inntektsvurdering = Inntektsvurdering(
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

        assertWarning("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.", 2.vedtaksperiode.filter())
        assertWarning("Inntektsmeldingen og vedtaksløsningen er uenige om beregningen av arbeidsgiverperioden. Undersøk hva som er riktig arbeidsgiverperiode.", 2.vedtaksperiode.filter())
        assertWarning("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.", 2.vedtaksperiode.filter())

        assertTrue(person.personLogg.warn().any { w ->
            w.toString().contains("Mottatt flere inntektsmeldinger")
        })
    }

}
