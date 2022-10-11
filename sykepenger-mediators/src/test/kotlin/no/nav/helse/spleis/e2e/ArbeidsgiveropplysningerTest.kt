package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ArbeidsgiveropplysningerTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender ut event TrengerArbeidsgiveropplysninger når vi ankommer AvventerInntektsmeldingEllerHistorikk`() = Toggle.Splarbeidsbros.enable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        Assertions.assertEquals(1, testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver").size)
    }

    @Test
    fun `sender ikke ut event TrengerArbeidsgiveropplysninger med toggle disabled`() = Toggle.Splarbeidsbros.disable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        Assertions.assertEquals(0, testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver").size)
    }
}