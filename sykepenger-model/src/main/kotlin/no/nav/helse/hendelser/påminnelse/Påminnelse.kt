package no.nav.helse.hendelser.påminnelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.TilstandType
import no.nav.helse.sak.VedtaksperiodeHendelse
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

    internal companion object {
        internal fun fraJson(json: JsonNode): Påminnelse? {
            val haveRequiredFields = listOf(
                "antallGangerPåminnet", "tilstand",
                "tilstandsendringstidspunkt", "påminnelsestidspunkt",
                "nestePåminnelsestidspunkt", "vedtaksperiodeId",
                "organisasjonsnummer", "fødselsnummer", "aktørId"
            ).all { json.hasNonNull(it) }
            if (!haveRequiredFields) return null
            return Påminnelse(json)
        }
    }

}
