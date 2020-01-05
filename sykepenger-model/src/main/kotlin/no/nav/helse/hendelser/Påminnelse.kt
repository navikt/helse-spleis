package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.TilstandType
import no.nav.helse.person.VedtaksperiodeHendelse
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

class Påminnelse private constructor(hendelseId: UUID, private val json: JsonNode) : ArbeidstakerHendelse(hendelseId, Hendelsetype.Påminnelse),
    VedtaksperiodeHendelse {

    private constructor(json: JsonNode) : this(UUID.randomUUID(), json)

    private val oldTilstander = mapOf(
        "NY_SØKNAD_MOTTATT" to TilstandType.MOTTATT_NY_SØKNAD,
        "SENDT_SØKNAD_MOTTATT" to TilstandType.MOTTATT_SENDT_SØKNAD,
        "INNTEKTSMELDING_MOTTATT" to TilstandType.MOTTATT_INNTEKTSMELDING,
        "KOMPLETT_SYKDOMSTIDSLINJE" to TilstandType.BEREGN_UTBETALING
    )

    val antallGangerPåminnet = json["antallGangerPåminnet"].intValue()
    val tilstand = json["tilstand"].textValue().let { oldTilstander[it] ?: TilstandType.valueOf(it) }
    val tilstandsendringstidspunkt = LocalDateTime.parse(json["tilstandsendringstidspunkt"].textValue())
    val påminnelsestidspunkt = LocalDateTime.parse(json["påminnelsestidspunkt"].textValue())

    val nestePåminnelsestidspunkt = LocalDateTime.parse(json["nestePåminnelsestidspunkt"].textValue())

    fun gjelderTilstand(tilstandType: TilstandType) = tilstandType == tilstand

    override fun aktørId(): String = json["aktørId"].textValue()

    override fun fødselsnummer() = json["fødselsnummer"].textValue()

    override fun organisasjonsnummer() = json["organisasjonsnummer"].textValue()
    override fun vedtaksperiodeId(): String = json["vedtaksperiodeId"].textValue()

    override fun rapportertdato() = påminnelsestidspunkt

    override fun toJson(): String {
        return json.toString()
    }

    class Builder : ArbeidstakerHendelseBuilder {
        override fun build(json: String): Påminnelse? {
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

    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
