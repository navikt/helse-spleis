package no.nav.helse.person

import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.SendtSøknad
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidstakerHendelseTest {

    private val nySøknad = nySøknadHendelse().toJson()
    private val sendtSøknad = sendtSøknadHendelse().toJson()
    private val inntektsmelding = inntektsmeldingHendelse().toJson()

    @Test
    internal fun `deserialize NySøknad`() {
        assertTrue(ArbeidstakerHendelse.fromJson(nySøknad) is NySøknad)
    }

    @Test
    internal fun `deserialize SendtSøknad`() {
        assertTrue(ArbeidstakerHendelse.fromJson(sendtSøknad) is SendtSøknad)
    }

    @Test
    internal fun `deserialize Inntektsmelding`() {
        assertTrue(ArbeidstakerHendelse.fromJson(inntektsmelding) is Inntektsmelding)
    }
}
