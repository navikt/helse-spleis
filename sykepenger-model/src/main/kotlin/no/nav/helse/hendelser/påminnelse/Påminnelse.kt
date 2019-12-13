package no.nav.helse.hendelser.påminnelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.TilstandType
import no.nav.helse.sak.VedtaksperiodeHendelse
import java.io.IOException
import java.time.LocalDateTime

class Påminnelse private constructor(private val json: JsonNode) : ArbeidstakerHendelse, VedtaksperiodeHendelse {

    internal val antallGangerPåminnet = json["antallGangerPåminnet"].intValue()
    internal val tilstand = TilstandType.valueOf(json["tilstand"].textValue())
    internal val tilstandsendringstidspunkt = LocalDateTime.parse(json["tilstandsendringstidspunkt"].textValue())
    internal val påminnelsestidspunkt = LocalDateTime.parse(json["påminnelsestidspunkt"].textValue())
    internal val nestePåminnelsestidspunkt = LocalDateTime.parse(json["nestePåminnelsestidspunkt"].textValue())

    override fun aktørId(): String = json["aktørId"].textValue()

    override fun fødselsnummer() = json["fødselsnummer"].textValue()

    override fun organisasjonsnummer() = json["organisasjonsnummer"].textValue()
    override fun vedtaksperiodeId(): String = json["vedtaksperiodeId"].textValue()

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
