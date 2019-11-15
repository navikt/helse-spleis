package no.nav.helse.person.hendelser.inntektsmelding

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class InntektsmeldingDeserializer: StdDeserializer<Inntektsmelding>(Inntektsmelding::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
            Inntektsmelding(objectMapper.readTree(parser))

}
