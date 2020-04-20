package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.PåminnelseMessage

internal class Påminnelser(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "påminnelse")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "antallGangerPåminnet",
                    "vedtaksperiodeId", "organisasjonsnummer",
                    "fødselsnummer", "aktørId"
                )
                it.require("tilstandsendringstidspunkt", JsonNode::asLocalDateTime)
                it.require("påminnelsestidspunkt", JsonNode::asLocalDateTime)
                it.require("nestePåminnelsestidspunkt", JsonNode::asLocalDateTime)
                it.requireAny("tilstand", TilstandType.values().map(Enum<*>::name))
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(PåminnelseMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Påminnelser", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Påminnelser", problems, context)
    }
}
