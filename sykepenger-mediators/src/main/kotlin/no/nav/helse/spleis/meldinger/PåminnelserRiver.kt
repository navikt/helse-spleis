package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.PåminnelseMessage

internal class PåminnelserRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "påminnelse"
    override val riverName = "Påminnelse"

    override fun validate(message: JsonMessage) {
        message.requireKey("antallGangerPåminnet", "vedtaksperiodeId",
            "organisasjonsnummer", "fødselsnummer", "aktørId")
        message.require("tilstandsendringstidspunkt", JsonNode::asLocalDateTime)
        message.require("påminnelsestidspunkt", JsonNode::asLocalDateTime)
        message.require("nestePåminnelsestidspunkt", JsonNode::asLocalDateTime)
        message.requireAny("tilstand", TilstandType.values().map(Enum<*>::name))
    }

    override fun createMessage(packet: JsonMessage) = PåminnelseMessage(packet)
}
