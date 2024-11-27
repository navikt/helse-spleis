package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.InfotrygdendringMessage

internal class InfotrygdendringerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "infotrygdendring"
    override val riverName = "Infotrygdendring"


    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer")
        message.require("endringsmeldingId", ::requireLong)
    }

    override fun createMessage(packet: JsonMessage) = InfotrygdendringMessage(
        packet, Meldingsporing(
        id = packet["@id"].asText().toUUID(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )

    private fun requireLong(node: JsonNode) {
        require(node.asLong() > 0)
    }
}
