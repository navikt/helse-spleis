package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage

internal class PersonAvstemmingRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "person_avstemming"
    override val riverName = "Person Avstemming"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "aktørId")
        message.require("fødselsnummer", ::requireLong)
        message.require("aktørId", ::requireLong)
    }

    override fun createMessage(packet: JsonMessage) = AvstemmingMessage(packet)

    private fun requireLong(node: JsonNode) {
        require(node.asLong() > 0)
    }
}
