package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.VedtaksperiodeHendelse
import java.time.YearMonth
import java.util.UUID
import kotlin.math.absoluteValue

class Vilkårsgrunnlag private constructor(
    hendelseId: UUID,
    private val behov: Behov
) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Vilkårsgrunnlag), VedtaksperiodeHendelse {

    private constructor(behov: Behov) : this(UUID.randomUUID(), behov)

    class Builder : ArbeidstakerHendelseBuilder {
        override fun build(json: String): Vilkårsgrunnlag? {
            return try {
                val behov = Behov.fromJson(json)
                if (!behov.erLøst() || Hendelsestype.Vilkårsgrunnlag != behov.hendelsetype())
                    return null
                Vilkårsgrunnlag(behov)
            } catch (err: Exception) {
                null
            }
        }
    }

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Vilkårsgrunnlag {
            return objectMapper.readTree(json).let {
                Vilkårsgrunnlag(
                    UUID.fromString(it["hendelseId"].textValue()),
                    Behov.fromJson(it["vilkårsgrunnlag"].toString())
                )
            }
        }
    }

    internal fun erEgenAnsatt(): Boolean {
        val løsning = behov.løsning() as Map<*, *>
        return løsning["EgenAnsatt"] as Boolean
    }

    override fun aktørId() = behov.aktørId()

    override fun fødselsnummer() = behov.fødselsnummer()

    override fun organisasjonsnummer() = behov.organisasjonsnummer()

    override fun vedtaksperiodeId() = behov.vedtaksperiodeId()

    override fun rapportertdato() = requireNotNull(behov.besvart())

    override fun toJson(): String {
        return objectMapper.convertValue<ObjectNode>(
            mapOf(
                "hendelseId" to hendelseId(),
                "type" to hendelsetype()
            )
        )
            .putRawValue("vilkårsgrunnlag", RawValue(behov.toJson()))
            .toString()
    }

    private fun beregnetÅrsInntekt(): Double {
        val løsning = behov.løsning() as Map<*, *>
        val inntektsmåneder = objectMapper.convertValue<List<Måned>>(løsning["Inntektsberegning"]!!)
        return inntektsmåneder
            .flatMap { it.inntektsliste }
            .sumByDouble { it.beløp }
    }

    private fun avviksprosentInntekt(månedsinntektFraInntektsmelding: Double) =
        ((månedsinntektFraInntektsmelding * 12) - beregnetÅrsInntekt()).absoluteValue / beregnetÅrsInntekt()

    private fun harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding: Double) =
        avviksprosentInntekt(månedsinntektFraInntektsmelding) > 0.25

    internal fun måHåndteresManuelt(månedsinntektFraInntektsmelding: Double): ModelVilkårsgrunnlag.Resultat {
        val grunnlag = ModelVilkårsgrunnlag.Grunnlagsdata(
            erEgenAnsatt(),
            beregnetÅrsInntekt(),
            avviksprosentInntekt(månedsinntektFraInntektsmelding)
        )

        return ModelVilkårsgrunnlag.Resultat(
            erEgenAnsatt() || harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding),
            grunnlag
        )
    }

    data class Måned(
        val årMåned: YearMonth,
        val inntektsliste: List<Inntekt>
    )

    data class Inntekt(
        val beløp: Double,
        val inntektstype: Inntektstype,
        val orgnummer: String?
    )

    enum class Inntektstype {
        LOENNSINNTEKT,
        NAERINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE
    }
}
