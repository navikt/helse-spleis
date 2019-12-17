package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SykdomshendelseDeserializerTest {

    private val deserializer = SykdomshendelseDeserializer()
    private val objectMapper = ObjectMapper()

    private val nySøknad = objectMapper.readTree(nySøknadHendelse().toJson())
    private val sendtSøknad = objectMapper.readTree(sendtSøknadHendelse().toJson())
    private val inntektsmelding = objectMapper.readTree(inntektsmeldingHendelse().toJson())

    @Test
    internal fun `deserialize NySøknad`() {
        assertTrue(deserializer.deserialize(nySøknad) is NySøknad)

        val oldJsonFormat = nySøknad.also {
            (it as ObjectNode).put("hendelsetype", SykdomshendelseType.NySøknadMottatt.name)
        }
        assertTrue(deserializer.deserialize(oldJsonFormat) is NySøknad)
    }

    @Test
    internal fun `deserialize SendtSøknad`() {
        assertTrue(deserializer.deserialize(sendtSøknad) is SendtSøknad)

        val oldJsonFormat = sendtSøknad.also {
            (it as ObjectNode).put("hendelsetype", SykdomshendelseType.SendtSøknadMottatt.name)
        }
        assertTrue(deserializer.deserialize(oldJsonFormat) is SendtSøknad)
    }

    @Test
    internal fun `deserialize Inntektsmelding`() {
        assertTrue(deserializer.deserialize(inntektsmelding) is Inntektsmelding)

        val oldJsonFormat = inntektsmelding.also {
            (it as ObjectNode).put("hendelsetype", SykdomshendelseType.InntektsmeldingMottatt.name)
        }
        assertTrue(deserializer.deserialize(oldJsonFormat) is Inntektsmelding)
    }
}
