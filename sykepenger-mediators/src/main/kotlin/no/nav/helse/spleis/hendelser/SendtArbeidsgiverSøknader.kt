package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.SendtSøknadArbeidsgiverMessage

internal class SendtArbeidsgiverSøknader(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "sendt_søknad_arbeidsgiver")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "fnr", "aktorId", "arbeidsgiver.orgnummer", "id", "egenmeldinger", "fravar")
                it.requireValue("status", "SENDT")
                it.require("fom", JsonNode::asLocalDate)
                it.require("tom", JsonNode::asLocalDate)
                it.requireArray("soknadsperioder") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireKey("sykmeldingsgrad")
                }
                it.require("sendtArbeidsgiver", JsonNode::asLocalDateTime)
                it.forbid("sendtNav")
            }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(SendtSøknadArbeidsgiverMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Sendt søknad arbeidsgiver", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Sendt søknad arbeidsgiver", problems, context)
    }
}
