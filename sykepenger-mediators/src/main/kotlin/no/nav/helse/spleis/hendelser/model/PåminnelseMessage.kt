package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(
    originalMessage: String,
    problems: MessageProblems) :
    HendelseMessage(originalMessage, problems) {

    init {
        requireValue("@event_name", "påminnelse")
        requireKey(
            "antallGangerPåminnet",
            "vedtaksperiodeId", "organisasjonsnummer",
            "fødselsnummer", "aktørId"
        )
        require("tilstandsendringstidspunkt", JsonNode::asLocalDateTime)
        require("påminnelsestidspunkt", JsonNode::asLocalDateTime)
        require("nestePåminnelsestidspunkt", JsonNode::asLocalDateTime)
        requireAny("tilstand", TilstandType.values().map(Enum<*>::name))
    }

    override val fødselsnummer: String get() = this["fødselsnummer"].asText()

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asPåminnelse(): Påminnelse {
        return Påminnelse(
            aktørId = this["aktørId"].asText(),
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            antallGangerPåminnet = this["antallGangerPåminnet"].asInt(),
            tilstand = TilstandType.valueOf(this["tilstand"].asText()),
            tilstandsendringstidspunkt = this["tilstandsendringstidspunkt"].asLocalDateTime(),
            påminnelsestidspunkt = this["påminnelsestidspunkt"].asLocalDateTime(),
            nestePåminnelsestidspunkt = this["nestePåminnelsestidspunkt"].asLocalDateTime()
        )
    }

    object Factory : MessageFactory<PåminnelseMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = PåminnelseMessage(message, problems)
    }
}
