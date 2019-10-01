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
import no.nav.helse.Event
import no.nav.helse.serde.safelyUnwrapDate
import java.lang.RuntimeException
import java.time.LocalDate


@JsonSerialize(using = SykmeldingSerializer::class)
@JsonDeserialize(using = SykmeldingDeserializer::class)
data class Sykmelding(val jsonNode: JsonNode): Event {

    val id = jsonNode["id"].asText()!!
    val aktørId = jsonNode["pasientAktoerId"].asText()!!
    val syketilfelleStartDato: LocalDate? get() = jsonNode["syketilfelleStartDato"].safelyUnwrapDate()
    val perioder: List<Periode> get() = jsonNode["perioder"].map { Periode(it) }
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

fun Sykmelding.gjelderFra(): LocalDate {
    return listOfNotNull(perioder.map { it.fom }.min(), syketilfelleStartDato).min() ?: throw RuntimeException("En sykmelding må ha en startdato")
}

fun Sykmelding.gjelderTil(): LocalDate = this.perioder.maxBy { it.tom }?.tom ?: throw RuntimeException("En sykmelding må ha en sluttdato")

@JsonSerialize(using = SykmeldingSerializer::class)
@JsonDeserialize(using = SykmeldingDeserializer::class)
data class Periode(val jsonNode: JsonNode) {
    val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
    val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
}

