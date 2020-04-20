package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.SimuleringMessage

internal class Simuleringer(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
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

                it.demandAll("@behov", Simulering)
                it.requireKey("@løsning.${Simulering.name}.status")
                it.require("@løsning.${Simulering.name}") { løsning ->
                    if (løsning["status"].asText() == "OK") {
                        it.requireKey("@løsning.${Simulering.name}.simulering")
                        it.interestedIn("@løsning.${Simulering.name}.feilmelding")
                    } else {
                        it.requireKey("@løsning.${Simulering.name}.feilmelding")
                        it.interestedIn("@løsning.${Simulering.name}.simulering")
                    }
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(SimuleringMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Simuleringer", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Simuleringer", problems, context)
    }
}
