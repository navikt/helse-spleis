package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VarselOmFlereInntektsmeldingerTest : AbstractEndToEndTest() {

    @Test
    fun `Prodbug - Feilaktig varsel om flere inntektsmeldinger`() {
        håndterSykmelding(Sykmeldingsperiode(22.mars(2021), 28.mars(2021)))
        håndterSøknad(22.mars(2021) til 28.mars(2021))

        håndterSykmelding(Sykmeldingsperiode(29.mars(2021), 5.april(2021)))
        håndterSøknad(29.mars(2021) til 5.april(2021))
        håndterSøknad(29.mars(2021) til 5.april(2021))

        håndterSykmelding(Sykmeldingsperiode(6.april(2021), 16.april(2021)))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(22.mars(2021) til 6.april(2021))
        )
        håndterSøknad(Sykdom(6.april(2021), 16.april(2021), 50.prosent))

        håndterVilkårsgrunnlag(vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        this@VarselOmFlereInntektsmeldingerTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(17.april(2021), 30.april(2021)))
        assertTrue(personlogg.varsel.none { w ->
            w.toString().contains("Mottatt flere inntektsmeldinger")
        })
    }

    @Test
    fun `Varsel om flere inntektsmeldinger hvis vi forlenger en avsluttet periode med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.februar til 16.februar),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        this@VarselOmFlereInntektsmeldingerTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@VarselOmFlereInntektsmeldingerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.mars til 16.mars)
        )
        håndterSøknad(Sykdom(1.mars, 20.mars, 50.prosent))

        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_IM_3), 2.vedtaksperiode.filter())
    }
}
