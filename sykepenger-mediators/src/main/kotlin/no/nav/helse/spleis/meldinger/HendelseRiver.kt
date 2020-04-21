package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import java.util.*

internal abstract class HendelseRiver(rapidsConnection: RapidsConnection, private val messageMediator: IMessageMediator) {
    protected val river = River(rapidsConnection)
    protected abstract val eventName: String
    protected abstract val riverName: String

    init {
        RiverImpl(river)
    }

    private fun validateHendelse(packet: JsonMessage) {
        packet.demandValue("@event_name", eventName)
        packet.require("@opprettet", JsonNode::asLocalDateTime)
        packet.require("@id") { UUID.fromString(it.asText()) }
    }

    protected abstract fun validate(packet: JsonMessage)
    protected abstract fun createMessage(packet: JsonMessage): HendelseMessage

    private inner class RiverImpl(river: River) : River.PacketListener {
        init {
            river.validate(::validateHendelse)
            river.validate(::validate)
            river.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            messageMediator.onRecognizedMessage(createMessage(packet), context)
        }

        override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
            messageMediator.onRiverSevere(riverName, error, context)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            messageMediator.onRiverError(riverName, problems, context)
        }
    }
}
