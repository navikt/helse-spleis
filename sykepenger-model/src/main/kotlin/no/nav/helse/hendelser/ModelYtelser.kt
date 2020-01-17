package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.VedtaksperiodeHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModelYtelser(
    hendelseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val sykepengehistorikk: ModelSykepengehistorikk,
    private val foreldrepenger: ModelForeldrepenger,
    private val rapportertdato: LocalDateTime,
    private val originalJson: String
) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Ytelser), VedtaksperiodeHendelse {
    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): ModelYtelser {
            return objectMapper.readTree(json).let {
                val aktivitetslogger = Aktivitetslogger()
                ModelYtelser(
                    hendelseId = UUID.fromString(it["hendelseId"].textValue()),
                    aktørId = it.path("ytelser").path("aktørId").asText(),
                    fødselsnummer = it.path("ytelser").path("fødselsnummer").asText(),
                    organisasjonsnummer = it.path("ytelser").path("organisasjonsnummer").asText(),
                    vedtaksperiodeId = it.path("ytelser").path("vedtaksperiodeId").asText(),
                    sykepengehistorikk = ModelSykepengehistorikk(
                        perioder = it.path("ytelser").path("@løsning").path("Sykepengehistorikk").map(::asPeriode),
                        aktivitetslogger = aktivitetslogger
                    ),
                    foreldrepenger = it.path("ytelser").path("@løsning").path("Foreldrepenger").let {
                        ModelForeldrepenger(
                            foreldrepengeytelse = it.path("Foreldrepengeytelse").takeIf(JsonNode::isObject)?.let(::asPeriode),
                            svangerskapsytelse = it.path("Svangerskapsytelse").takeIf(JsonNode::isObject)?.let(::asPeriode),
                            aktivitetslogger = aktivitetslogger
                        )
                    },
                    rapportertdato = it.path("ytelser").path("@besvart").asLocalDateTime(),
                    originalJson = objectMapper.writeValueAsString(it.path("ytelser"))
                )
            }
        }

        fun lagBehov(
            vedtaksperiodeId: UUID,
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            utgangspunktForBeregningAvYtelse: LocalDate
        ): Behov {
            val params = mutableMapOf(
                "utgangspunktForBeregningAvYtelse" to utgangspunktForBeregningAvYtelse
            )

            return Behov.nyttBehov(
                hendelsestype = Hendelsestype.Ytelser,
                behov = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                additionalParams = params
            )
        }

        private fun JsonNode.asLocalDate() =
            asText().let { LocalDate.parse(it) }

        private fun JsonNode.asLocalDateTime() =
            asText().let { LocalDateTime.parse(it) }

        private fun asPeriode(jsonNode: JsonNode) =
            jsonNode.path("fom").asLocalDate() to jsonNode.path("tom").asLocalDate()

    }

    internal fun sykepengehistorikk() = sykepengehistorikk

    internal fun foreldrepenger() = foreldrepenger

    override fun rapportertdato(): LocalDateTime {
        return rapportertdato
    }

    override fun aktørId(): String {
        return aktørId
    }

    override fun fødselsnummer(): String {
        return fødselsnummer
    }

    override fun organisasjonsnummer(): String {
        return organisasjonsnummer
    }

    override fun toJson(): String {
        return objectMapper.writeValueAsString(
            objectMapper.convertValue<ObjectNode>(
                mapOf(
                    "hendelseId" to hendelseId(),
                    "type" to hendelsetype()
                )
            ).putRawValue("ytelser", RawValue(originalJson))
        )
    }

    override fun vedtaksperiodeId(): String {
        return vedtaksperiodeId
    }
}
