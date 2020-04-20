package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.hendelser.model.PåminnelseMessage

internal class Påminnelser(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "påminnelse"
    override val riverName = "Påminnelse"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("antallGangerPåminnet", "vedtaksperiodeId",
            "organisasjonsnummer", "fødselsnummer", "aktørId")
        packet.require("tilstandsendringstidspunkt", JsonNode::asLocalDateTime)
        packet.require("påminnelsestidspunkt", JsonNode::asLocalDateTime)
        packet.require("nestePåminnelsestidspunkt", JsonNode::asLocalDateTime)
        packet.requireAny("tilstand", TilstandType.values().map(Enum<*>::name))
    }

    override fun createMessage(packet: JsonMessage) = PåminnelseMessage(packet)
}
