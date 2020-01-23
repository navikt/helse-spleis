package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behovstype
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogger
import no.nav.helse.person.VedtaksperiodeHendelse
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.math.absoluteValue

class ModelVilkårsgrunnlag(
    hendelseId: UUID,
    private val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val rapportertDato: LocalDateTime,
    private val inntektsmåneder: List<Måned>,
    private val erEgenAnsatt: Boolean,
    private val aktivitetslogger: Aktivitetslogger,
    private val originalJson: String
) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Vilkårsgrunnlag),VedtaksperiodeHendelse, IAktivitetslogger by aktivitetslogger {
    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): ModelVilkårsgrunnlag {
            return objectMapper.readTree(json).let {
                ModelVilkårsgrunnlag(
                    UUID.fromString(it["hendelseId"].textValue()),
                    it["vilkårsgrunnlag"]["vedtaksperiodeId"].asText(),
                    it["vilkårsgrunnlag"]["aktørId"].asText(),
                    it["vilkårsgrunnlag"]["fødselsnummer"].asText(),
                    it["vilkårsgrunnlag"]["organisasjonsnummer"].asText(),
                    it["vilkårsgrunnlag"]["@besvart"].asLocalDateTime(),
                    it["vilkårsgrunnlag"]["@løsning.${Behovstype.Inntektsberegning.name}"].map {
                        Måned(
                            it["årMåned"].asYearMonth(),
                            it["inntektsliste"].map { Inntekt(it["beløp"].asDouble()) }
                        )
                    },
                    it["vilkårsgrunnlag"]["@løsning.${Behovstype.EgenAnsatt.name}"].asBoolean(),
                    Aktivitetslogger(),
                    objectMapper.writeValueAsString(it.path("vilkårsgrunnlag"))
                )
            }
        }

        private fun JsonNode.asYearMonth() =
            asText().let { YearMonth.parse(it) }

        private fun JsonNode.asLocalDateTime() =
            asText().let { LocalDateTime.parse(it) }
    }

    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun rapportertdato() = rapportertDato
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer
    override fun toJson(): String {
        return objectMapper.convertValue<ObjectNode>(
            mapOf(
                "hendelseId" to hendelseId(),
                "type" to hendelsetype()
            )
        )
            .putRawValue("vilkårsgrunnlag", RawValue(originalJson))
            .toString()
    }

    private fun beregnetÅrsInntekt(): Double {
        return inntektsmåneder
            .flatMap { it.inntektsliste }
            .sumByDouble { it.beløp }
    }

    private fun avviksprosentInntekt(månedsinntektFraInntektsmelding: Double) =
        ((månedsinntektFraInntektsmelding * 12) - beregnetÅrsInntekt()).absoluteValue / beregnetÅrsInntekt()

    internal fun harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding: Double) =
        avviksprosentInntekt(månedsinntektFraInntektsmelding) > 0.25

    internal fun måHåndteresManuelt(månedsinntektFraInntektsmelding: Double): Resultat {
        val grunnlag = Grunnlagsdata(
            erEgenAnsatt,
            beregnetÅrsInntekt(),
            avviksprosentInntekt(månedsinntektFraInntektsmelding)
        )

        return Resultat(erEgenAnsatt || harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding), grunnlag)
    }

    data class Måned(
        val årMåned: YearMonth,
        val inntektsliste: List<Inntekt>
    )

    data class Inntekt(
        val beløp: Double
    )

    data class Resultat(
        val resultat: Boolean,
        val grunnlagsdata: Grunnlagsdata
    )

    data class Grunnlagsdata(
        val erEgenAnsatt: Boolean,
        val beregnetÅrsinntektFraInntektskomponenten: Double,
        val avviksprosent: Double
    )
}

