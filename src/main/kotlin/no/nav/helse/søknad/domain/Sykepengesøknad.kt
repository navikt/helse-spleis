package no.nav.helse.søknad.domain

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykmelding.domain.Periode
import java.time.LocalDate

@JsonSerialize(using = SykepengesøknadSerializer::class)
@JsonDeserialize(using = SykepengesøknadDeserializer::class)
data class Sykepengesøknad(val jsonNode: JsonNode) {

    val id = jsonNode["id"].asText()!!

    val sykmeldingId = jsonNode["sykmeldingId"].asText()!!

    val aktørId = jsonNode["aktorId"].asText()!!
    val fom = jsonNode["fom"].asText().let { LocalDate.parse(it) }
    val tom = jsonNode["tom"].asText().let { LocalDate.parse(it) }
    val egenmeldinger = jsonNode["egenmeldinger"]?.map { Periode(it) }
        ?: emptyList()
    val arbeidGjenopptatt = jsonNode["arbeidGjenopptatt"].safelyUnwrapDate()
    val korrigerer get() = jsonNode["korrigerer"]?.asText()
}

class SykepengesøknadSerializer : StdSerializer<Sykepengesøknad>(Sykepengesøknad::class.java) {
    override fun serialize(søknad: Sykepengesøknad?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(søknad?.jsonNode)
    }
}

class SykepengesøknadDeserializer : StdDeserializer<Sykepengesøknad>(Sykepengesøknad::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
        Sykepengesøknad(objectMapper.readTree(parser))

}
