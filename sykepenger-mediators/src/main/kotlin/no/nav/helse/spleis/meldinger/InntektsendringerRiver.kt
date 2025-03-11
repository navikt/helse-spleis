package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.InfotrygdendringMessage

internal class InntektsendringerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "inntektsendringer"
    override val riverName = "Inntektsendringer"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer")
    }

    override fun precondition(packet: JsonMessage) {
        packet.require("inntektsendringFom", JsonNode::asLocalDate)
    }

    override fun createMessage(packet: JsonMessage) = InfotrygdendringMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
