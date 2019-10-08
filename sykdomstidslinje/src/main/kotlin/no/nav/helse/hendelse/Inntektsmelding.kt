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
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.objectMapper
import java.time.LocalDate
import java.time.LocalDateTime

data class Inntektsmelding(val jsonNode: JsonNode) : Event, Sykdomshendelse {

    val arbeidsgiverFnr: String? get() = jsonNode["arbeidsgiverFnr"]?.textValue()

    val førsteFraværsdag: LocalDate get() = LocalDate.parse(jsonNode["forsteFravarsdag"].textValue())
    val rapportertDato: LocalDateTime get() = LocalDateTime.parse(jsonNode["rapportertDato"].textValue())

    val ferie
        get() = jsonNode["ferie"].map {
            Periode(it)
        }

    val inntektsmeldingId = jsonNode["inntektsmeldingId"].asText() as String

    val arbeidstakerAktorId = jsonNode["arbeidstakerAktorId"].textValue() as String

    val virksomhetsnummer: String? get() = jsonNode["virksomhetsnummer"]?.textValue()

    val arbeidsgiverAktorId: String? get() = jsonNode["arbeidsgiverAktorId"]?.textValue()

    val arbeidsgiverperioder
        get() = jsonNode["arbeidsgiverperioder"].map {
            Periode(it)
        }

    val sisteDagIArbeidsgiverPeriode
        get() = arbeidsgiverperioder.maxBy {
            it.tom
        }?.tom

    override fun rapportertdato() = rapportertDato

    override fun eventType() = Event.Type.Inntektsmelding

    override fun aktørId(): String = arbeidstakerAktorId

    override fun organisasjonsnummer(): String = virksomhetsnummer ?: arbeidsgiverFnr
    ?: throw RuntimeException("Inntektsmelding mangler orgnummer og arbeidsgiver fnr")

    override fun sykdomstidslinje(): Sykdomstidslinje {
        val arbeidsgiverperiodetidslinjer = arbeidsgiverperioder
            .map { Sykdomstidslinje.sykedager(it.fom, it.tom, this) }
        val ferietidslinjer = ferie
            .map { Sykdomstidslinje.ferie(it.fom, it.tom, this) }
        val førsteFraværsdagTidslinje = listOf(Sykdomstidslinje.sykedager(gjelder = førsteFraværsdag, hendelse = this))

        return (førsteFraværsdagTidslinje + arbeidsgiverperiodetidslinjer + ferietidslinjer)
            .reduce { resultatTidslinje, delTidslinje -> resultatTidslinje + delTidslinje }
    }

    data class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate
    }

    override fun toJson(): JsonNode = jsonNode
}

class InntektsmeldingSerializer : StdSerializer<Inntektsmelding>(Inntektsmelding::class.java) {
    override fun serialize(sykmelding: Inntektsmelding?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(sykmelding?.jsonNode)
    }
}

class InntektsmeldingDeserializer : StdDeserializer<Inntektsmelding>(Inntektsmelding::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
        Inntektsmelding(objectMapper.readTree(parser))

}
