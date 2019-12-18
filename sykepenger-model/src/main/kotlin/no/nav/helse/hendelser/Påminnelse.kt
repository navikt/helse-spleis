package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.TilstandType
import no.nav.helse.sak.VedtaksperiodeHendelse
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

class Påminnelse private constructor(hendelseId: UUID, private val json: JsonNode) : ArbeidstakerHendelse(hendelseId, Hendelsetype.Påminnelse), VedtaksperiodeHendelse {

    private constructor(json: JsonNode) : this(UUID.randomUUID(), json)

    val antallGangerPåminnet = json["antallGangerPåminnet"].intValue()
    val tilstand = TilstandType.valueOf(json["tilstand"].textValue())
    val tilstandsendringstidspunkt = LocalDateTime.parse(json["tilstandsendringstidspunkt"].textValue())
    val påminnelsestidspunkt = LocalDateTime.parse(json["påminnelsestidspunkt"].textValue())
    val nestePåminnelsestidspunkt = LocalDateTime.parse(json["nestePåminnelsestidspunkt"].textValue())

    override fun aktørId(): String = json["aktørId"].textValue()

    override fun fødselsnummer() = json["fødselsnummer"].textValue()

    override fun organisasjonsnummer() = json["organisasjonsnummer"].textValue()
    override fun vedtaksperiodeId(): String = json["vedtaksperiodeId"].textValue()

    override fun rapportertdato() = påminnelsestidspunkt

    override fun toJson(): String {
        return json.toString()
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fraJson(json: String): Påminnelse? {
            val jsonNode = try {
                objectMapper.readTree(json)
            } catch (err: IOException) {
                return null
            }

            val haveRequiredFields = listOf(
                "antallGangerPåminnet", "tilstand",
                "tilstandsendringstidspunkt", "påminnelsestidspunkt",
                "nestePåminnelsestidspunkt", "vedtaksperiodeId",
                "organisasjonsnummer", "fødselsnummer", "aktørId"
            ).all { jsonNode.hasNonNull(it) }
            if (!haveRequiredFields) return null
            return Påminnelse(jsonNode)
        }
    }

}
