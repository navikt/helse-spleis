package no.nav.helse.hendelse

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate

data class Inntektsmelding(val jsonNode: JsonNode): Event {
    override fun eventType() = Event.Type.Inntektsmelding

    val inntektsmeldingId = jsonNode["inntektsmeldingId"].asText() as String

    val arbeidstakerAktorId = jsonNode["arbeidstakerAktorId"].textValue() as String

    val virksomhetsnummer: String? get() = jsonNode["virksomhetsnummer"]?.textValue()

    val arbeidsgiverAktorId: String? get() = jsonNode["arbeidsgiverAktorId"]?.textValue()

    val arbeidsgiverperioder get() = jsonNode["arbeidsgiverperioder"].map {
        Periode(it)
    }

    val sisteDagIArbeidsgiverPeriode get() = arbeidsgiverperioder.maxBy {
        it.tom
    }?.tom

    data class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate
    }

}

class InntektsmeldingSerializer: StdSerializer<Inntektsmelding>(Inntektsmelding::class.java) {
    override fun serialize(sykmelding: Inntektsmelding?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(sykmelding?.jsonNode)
    }
}

class InntektsmeldingDeserializer: StdDeserializer<Inntektsmelding>(Inntektsmelding::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
            Inntektsmelding(objectMapper.readTree(parser))

}
