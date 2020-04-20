package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.ManuellSaksbehandlingMessage

internal class ManuelleSaksbehandlinger(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "@behov", "@final", "@løsning",
                    "aktørId", "fødselsnummer",
                    "organisasjonsnummer", "vedtaksperiodeId",
                    "tilstand"
                )
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.require("@besvart", JsonNode::asLocalDateTime)
                it.requireValue("@final", true)

                it.demandAll("@behov", Godkjenning)
                it.requireKey("@løsning.${Godkjenning.name}.godkjent")
                it.requireKey("saksbehandlerIdent")
                it.require("godkjenttidspunkt", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(ManuellSaksbehandlingMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Manuell saksbehandling", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Manuell saksbehandling", problems, context)
    }
}
