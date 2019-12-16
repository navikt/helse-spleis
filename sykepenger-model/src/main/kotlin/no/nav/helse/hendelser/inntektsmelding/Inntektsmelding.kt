package no.nav.helse.hendelser.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.serde.safelyUnwrapDate
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

@JsonDeserialize(using = InntektsmeldingDeserializer::class)
data class Inntektsmelding(val jsonNode: JsonNode) {
    companion object {
        private val log = LoggerFactory.getLogger(Companion::class.java)

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Inntektsmelding? {
            return try {
                Inntektsmelding(objectMapper.readTree(json))
            } catch (err: IOException) {
                log.info("kunne ikke lese inntektsmelding som json: ${err.message}", err)
                null
            }
        }
    }

    val arbeidsgiverFnr: String? get() = jsonNode["arbeidsgiverFnr"]?.textValue()
    val førsteFraværsdag: LocalDate get() = LocalDate.parse(jsonNode["foersteFravaersdag"].textValue())
    val mottattDato: LocalDateTime get() = LocalDateTime.parse(jsonNode["mottattDato"].textValue())
    val ferie get() = jsonNode["ferieperioder"]?.map { Periode(it) } ?: emptyList()
    val inntektsmeldingId = jsonNode["inntektsmeldingId"].asText() as String
    val arbeidstakerAktorId = jsonNode["arbeidstakerAktorId"].textValue() as String
    val arbeidstakerFnr = jsonNode["arbeidstakerFnr"].textValue() as String
    val virksomhetsnummer: String? get() = jsonNode["virksomhetsnummer"]?.textValue()
    val arbeidsgiverAktorId: String? get() = jsonNode["arbeidsgiverAktorId"]?.textValue()
    val arbeidsgiverperioder get() = jsonNode["arbeidsgiverperioder"]?.map { Periode(it) } ?: emptyList()
    val beregnetInntekt
        get() = jsonNode["beregnetInntekt"]
            ?.takeUnless { it.isNull }
            ?.textValue()?.toBigDecimal()
    val refusjon get() = Refusjon(jsonNode["refusjon"])
    val endringIRefusjoner
        get() = jsonNode["endringIRefusjoner"]
            .mapNotNull { it["endringsdato"].safelyUnwrapDate() }

    fun kanBehandles() = jsonNode["mottattDato"] != null
            && jsonNode["foersteFravaersdag"] != null
            && jsonNode["virksomhetsnummer"] != null && !jsonNode["virksomhetsnummer"].isNull
            && jsonNode["beregnetInntekt"] != null && !jsonNode["beregnetInntekt"].isNull
            && jsonNode["arbeidstakerFnr"] != null
            && jsonNode["refusjon"]?.let { Refusjon(it) }?.beloepPrMnd == beregnetInntekt ?: false

    data class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate
    }

    data class Refusjon(val jsonNode: JsonNode) {
        val opphoersdato get() = jsonNode["opphoersdato"].safelyUnwrapDate()
        val beloepPrMnd get() = jsonNode["beloepPrMnd"]?.textValue()?.toBigDecimal()
    }
}
