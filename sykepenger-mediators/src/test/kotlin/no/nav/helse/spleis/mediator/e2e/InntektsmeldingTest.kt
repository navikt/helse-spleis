package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Test

internal class InntektsmeldingTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Håndterer portalinntektsmelding uten inntektsdato`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(
            arbeidsgiverperiode = listOf(Periode(1.januar, 31.januar)),
            førsteFraværsdag = 1.januar
        )
        sendVilkårsgrunnlag(vedtaksperiodeIndeks = 0)
    }
}
