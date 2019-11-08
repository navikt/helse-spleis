package no.nav.helse.person.hendelser.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.helse.spleis.serde.safelyUnwrapDate
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

    val beregnetInntekt
        get() = jsonNode["beregnetInntekt"]?.let {
            if (it.isNull) null else it.textValue().toBigDecimal()
        }

    val refusjon
        get() = Refusjon(jsonNode["refusjon"])

    val endringIRefusjoner
        get() = jsonNode["endringIRefusjoner"]
                .mapNotNull { it["endringsdato"].safelyUnwrapDate() }

    fun kanBehandles(): Boolean {
        return jsonNode["mottattDato"] != null
                && jsonNode["foersteFravaersdag"] != null
                && jsonNode["virksomhetsnummer"] != null && !jsonNode["virksomhetsnummer"].isNull
                && jsonNode["beregnetInntekt"] != null && !jsonNode["beregnetInntekt"].isNull
                && jsonNode["refusjon"]?.let { Refusjon(it) }?.beloepPrMnd == beregnetInntekt ?: false
    }

    data class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate

    }

    data class Refusjon(val jsonNode: JsonNode) {
        val opphoersdato get() = jsonNode["opphoersdato"].safelyUnwrapDate()
        val beloepPrMnd get() = jsonNode["beloepPrMnd"]?.textValue()?.toBigDecimal()
    }

    fun toJson(): JsonNode = jsonNode
}
