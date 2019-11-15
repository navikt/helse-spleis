package no.nav.helse.hendelser.søknad

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal class SykepengesøknadDeserializer : StdDeserializer<Sykepengesøknad>(Sykepengesøknad::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
            objectMapper.readTree<JsonNode>(parser).let {
                when (it["status"].textValue()) {
                    "NY" -> Sykepengesøknad(it)
                    "FREMTIDIG" -> Sykepengesøknad(it)
                    "SENDT" -> Sykepengesøknad(it)
                    else -> throw IllegalArgumentException("Kan ikke håndtere søknad med type ${it["type"].textValue()}.")
                }
            }

}
