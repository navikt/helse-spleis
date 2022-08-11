package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsgiverMessage

internal class SendtArbeidsgiverSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_arbeidsgiver"
    override val riverName = "Sendt søknad arbeidsgiver"

    override fun validate(message: JsonMessage) {
        message.requireKey("id")
        message.requireArray("egenmeldinger") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("papirsykmeldinger") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("fravar") {
            requireAny("type", listOf("UTDANNING_FULLTID", "UTDANNING_DELTID", "PERMISJON", "FERIE", "UTLANDSOPPHOLD"))
            require("fom", JsonNode::asLocalDate)
            interestedIn("tom") { it.asLocalDate() }
        }
        message.interestedIn("arbeidGjenopptatt", "andreInntektskilder", "permitteringer", "merknaderFraSykmelding", "korrigerer")
        message.requireValue("status", "SENDT")
        message.require("sendtArbeidsgiver", JsonNode::asLocalDateTime)
        message.forbid("sendtNav")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadArbeidsgiverMessage(packet)
}
