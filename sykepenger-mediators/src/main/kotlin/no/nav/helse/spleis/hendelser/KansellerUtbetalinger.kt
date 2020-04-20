package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.KansellerUtbetalingMessage

internal class KansellerUtbetalinger(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kanseller_utbetaling")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "aktørId", "fødselsnummer", "organisasjonsnummer", "fagsystemId", "saksbehandler")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(KansellerUtbetalingMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Kanseller utbetaling", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Kanseller utbetaling", problems, context)
    }
}
