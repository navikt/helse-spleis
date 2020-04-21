package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SendtSøknadNavMessage

internal class SendtNavSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_nav"
    override val riverName = "Sendt søknad Nav"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("id")
        packet.requireArray("egenmeldinger") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        packet.requireArray("fravar") {
            requireAny("type", listOf("UTDANNING_FULLTID", "UTDANNING_DELTID", "PERMISJON", "FERIE", "UTLANDSOPPHOLD"))
            require("fom", JsonNode::asLocalDate)
            interestedIn("tom") { it.asLocalDate() }
        }
        packet.require("sendtNav", JsonNode::asLocalDateTime)
        packet.interestedIn("arbeidGjenopptatt", "andreInntektskilder", "permitteringer")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadNavMessage(packet)
}
