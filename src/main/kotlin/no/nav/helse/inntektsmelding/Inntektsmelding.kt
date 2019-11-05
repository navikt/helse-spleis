package no.nav.helse.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate
import java.time.LocalDateTime

@JsonDeserialize(using = InntektsmeldingDeserializer::class)
data class Inntektsmelding(val jsonNode: JsonNode) {

    val arbeidsgiverFnr: String? get() = jsonNode["arbeidsgiverFnr"]?.textValue()

    val førsteFraværsdag: LocalDate get() = LocalDate.parse(jsonNode["foersteFravaersdag"].textValue())

    val mottattDato: LocalDateTime get() = LocalDateTime.parse(jsonNode["mottattDato"].textValue())

    val ferie
        get() = jsonNode["ferieperioder"]?.map {
            Periode(it)
        } ?: emptyList()

    val inntektsmeldingId = jsonNode["inntektsmeldingId"].asText() as String

    val arbeidstakerAktorId = jsonNode["arbeidstakerAktorId"].textValue() as String

    val virksomhetsnummer: String? get() = jsonNode["virksomhetsnummer"]?.textValue()

    val arbeidsgiverAktorId: String? get() = jsonNode["arbeidsgiverAktorId"]?.textValue()

    val arbeidsgiverperioder
        get() = jsonNode["arbeidsgiverperioder"]?.map {
            Periode(it)
        } ?: emptyList()

    val sisteDagIArbeidsgiverPeriode
        get() = arbeidsgiverperioder.maxBy {
            it.tom
        }?.tom

    fun kanBehandles(): Boolean {
        return jsonNode["mottattDato"] != null
                && jsonNode["foersteFravaersdag"] != null
    }
    data class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate

    }
    fun toJson(): JsonNode = jsonNode
}
