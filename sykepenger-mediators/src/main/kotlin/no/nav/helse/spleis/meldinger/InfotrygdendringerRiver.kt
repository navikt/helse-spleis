package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.InfotrygdendringMessage

internal class InfotrygdendringerRiver (
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
    ) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "infotrygdendring"
    override val riverName = "Infotrygdendring"


    override fun validate(message: JsonMessage) {
        message.requireKey(
            "aktørId", "fødselsnummer")
        message.require("endringsmeldingId", ::requireLong)
    }

    override fun createMessage(packet: JsonMessage) = InfotrygdendringMessage(packet)

    private fun requireLong(node: JsonNode) {
        require(node.asLong() > 0)
    }
}