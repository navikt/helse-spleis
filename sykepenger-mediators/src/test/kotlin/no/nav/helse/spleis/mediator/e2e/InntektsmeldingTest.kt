package no.nav.helse.spleis.mediator.e2e

import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

internal class InntektsmeldingTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Håndterer portalinntektsmelding uten inntektsdato`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(
            arbeidsgiverperiode = listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar
        )
        sendVilkårsgrunnlag(vedtaksperiodeIndeks = 0)
    }

    @Test
    fun `inntektsmelding_handtert har listen vedtaksperioderMedSammeFørsteFraværsdag med riktige vedtaksperiode id'er`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 10.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 10.januar, sykmeldingsgrad = 100)))

        sendNySøknad(SoknadsperiodeDTO(fom = 11.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 11.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(
            arbeidsgiverperiode = listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar
        )

        val inntektsmeldingHåndtertEvent = testRapid.inspektør.meldinger("inntektsmelding_håndtert").first()
        assertNotNull(inntektsmeldingHåndtertEvent)
        val perioderIder = listOf(testRapid.inspektør.vedtaksperiodeId(0), testRapid.inspektør.vedtaksperiodeId(1))

        assertEquals(inntektsmeldingHåndtertEvent.path("vedtaksperioderMedSammeFørsteFraværsdag").asIterable().map { UUID.fromString(it.asText()) }, perioderIder)
    }
}
