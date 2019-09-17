package no.nav.helse.inntektsmelding.serde

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

val inntektsmeldingObjectMapper = JacksonJsonConfig.opprettObjectMapper()

fun InntektsmeldingSerde() =
        Serdes.serdeFrom(InntektsmeldingSerializer(inntektsmeldingObjectMapper), InnteksmeldingDeserializer(inntektsmeldingObjectMapper))

class InnteksmeldingDeserializer(private val objectMapper: ObjectMapper): Deserializer<Inntektsmelding> {
    private val log = LoggerFactory.getLogger(InnteksmeldingDeserializer::class.java)

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) { }

    override fun deserialize(topic: String?, data: ByteArray?): Inntektsmelding? {
        return data?.let {
            try {
                objectMapper.readValue(it, Inntektsmelding::class.java)
            } catch (e: Exception) {
                log.warn("Not a valid json",e)
                null
            }
        }
    }

    override fun close() { }
}

class InntektsmeldingSerializer(private val objectMapper: ObjectMapper): Serializer<Inntektsmelding> {
    private val log = LoggerFactory.getLogger(InntektsmeldingSerializer::class.java)

    override fun serialize(topic: String?, data: Inntektsmelding?): ByteArray? {
        return data?.let {
            try {
                objectMapper.writeValueAsBytes(it)
            } catch (e: Exception) {
                log.warn("Could not serialize Inntektsmelding",e)
                null
            }
        }
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) { }
    override fun close() { }

}