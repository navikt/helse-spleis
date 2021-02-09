package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class InntektsmeldingPåKjølTest : AbstractEndToEndTest() {

    @Test
    fun `publiserer event når vi mottar inntektsmelding uten at det fins perioder`() {
        val inntektsmeldingId = UUID.randomUUID()
        håndterInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), inntektsmeldingId = inntektsmeldingId)

        assertEquals(inntektsmeldingId, observatør.inntektsmeldingerLagtPåKjøl[0])
    }

    @Test
    fun `publiserer event når inntektsmelding ikke treffer noen periode`() {
        val inntektsmeldingId = UUID.randomUUID()
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)
        håndterInntektsmelding(listOf(Periode(fom = 1.mars, tom = 16.mars)), inntektsmeldingId = inntektsmeldingId)

        assertEquals(inntektsmeldingId, observatør.inntektsmeldingerLagtPåKjøl[0])
    }

    @Test
    fun `publiserer ikke event når inntektsmelding treffer en vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))

        assertEquals(0, observatør.inntektsmeldingerLagtPåKjøl.size)
    }
}
