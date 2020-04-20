package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.SendtSøknadNavMessage

internal class SendtNavSøknader(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "sendt_søknad_nav")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "fnr", "aktorId", "arbeidsgiver.orgnummer", "id")
                it.requireValue("status", "SENDT")
                it.require("fom", JsonNode::asLocalDate)
                it.require("tom", JsonNode::asLocalDate)
                it.requireArray("soknadsperioder") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireKey("sykmeldingsgrad")
                }
                it.requireArray("egenmeldinger") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                }
                it.requireArray("fravar") {
                    requireAny("type", listOf("UTDANNING_FULLTID", "UTDANNING_DELTID", "PERMISJON", "FERIE", "UTLANDSOPPHOLD"))
                    require("fom", JsonNode::asLocalDate)
                    require("tom") {
                        if (!it.isMissingOrNull()) it.asLocalDate()
                    }
                }
                it.require("sendtNav", JsonNode::asLocalDateTime)
                it.interestedIn("arbeidGjenopptatt", "andreInntektskilder", "permitteringer")
            }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(SendtSøknadNavMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Sendt søknad Nav", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Sendt søknad Nav", problems, context)
    }
}
