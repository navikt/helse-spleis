package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage

internal class PersonAvstemmingRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "person_avstemming"
    override val riverName = "Person Avstemming"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("fødselsnummer", "aktørId")
        packet.require("fødselsnummer", ::requireLong)
        packet.require("aktørId", ::requireLong)
    }

    override fun createMessage(packet: JsonMessage) = AvstemmingMessage(JsonMessageDelegate(packet))

    private fun requireLong(node: JsonNode) {
        require(node.asLong() > 0)
    }
}
