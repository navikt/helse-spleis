package no.nav.helse.sak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.SendtSøknad
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidstakerHendelseTest {

    private val objectMapper = ObjectMapper()
    private val nySøknad = nySøknadHendelse().toJson()
    private val sendtSøknad = sendtSøknadHendelse().toJson()
    private val inntektsmelding = inntektsmeldingHendelse().toJson()

    @Test
    internal fun `deserialize NySøknad`() {
        assertTrue(ArbeidstakerHendelse.fromJson(nySøknad) is NySøknad)

        val oldJsonFormat = nySøknad.also {
            (objectMapper.readTree(it) as ObjectNode).put("hendelsetype", ArbeidstakerHendelse.SykdomshendelseType.NySøknadMottatt.name)
        }
        assertTrue(ArbeidstakerHendelse.fromJson(oldJsonFormat) is NySøknad)
    }

    @Test
    internal fun `deserialize SendtSøknad`() {
        assertTrue(ArbeidstakerHendelse.fromJson(sendtSøknad) is SendtSøknad)

        val oldJsonFormat = sendtSøknad.also {
            (objectMapper.readTree(it) as ObjectNode).put("hendelsetype", ArbeidstakerHendelse.SykdomshendelseType.SendtSøknadMottatt.name)
        }
        assertTrue(ArbeidstakerHendelse.fromJson(oldJsonFormat) is SendtSøknad)
    }

    @Test
    internal fun `deserialize Inntektsmelding`() {
        assertTrue(ArbeidstakerHendelse.fromJson(inntektsmelding) is Inntektsmelding)

        val oldJsonFormat = inntektsmelding.also {
            (objectMapper.readTree(it) as ObjectNode).put("hendelsetype", ArbeidstakerHendelse.SykdomshendelseType.InntektsmeldingMottatt.name)
        }
        assertTrue(ArbeidstakerHendelse.fromJson(oldJsonFormat) is Inntektsmelding)
    }
}
