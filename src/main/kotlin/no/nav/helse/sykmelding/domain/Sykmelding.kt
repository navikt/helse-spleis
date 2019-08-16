package no.nav.helse.sykmelding.domain

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
import no.nav.helse.søknad.domain.safelyUnwrapDate
import java.time.LocalDate


@JsonSerialize(using = SykmeldingSerializer::class)
@JsonDeserialize(using = SykmeldingDeserializer::class)
data class Sykmelding(val jsonNode: JsonNode) {

    val id = jsonNode["id"].asText()!!

    val aktørId = jsonNode["pasientAktoerId"].asText()!!

    val syketilfelleStartDato: LocalDate? = jsonNode["syketilfelleStartDato"].safelyUnwrapDate()
    val perioder: List<Periode> = jsonNode["perioder"].map {
        Periode(
            fom = LocalDate.parse(it["fom"].textValue()),
            tom = LocalDate.parse(it["tom"].textValue())
        )
    }
}

class SykmeldingSerializer : StdSerializer<Sykmelding>(Sykmelding::class.java) {
    override fun serialize(sykmelding: Sykmelding?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(sykmelding?.jsonNode)
    }
}

class SykmeldingDeserializer : StdDeserializer<Sykmelding>(Sykmelding::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
        Sykmelding(objectMapper.readTree(parser))

}

fun Sykmelding.sykmeldingGjelderFra(): LocalDate? {
    return listOfNotNull(perioder.map { it.fom }.min(), syketilfelleStartDato).min()
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
)
