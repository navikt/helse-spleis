package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.MinimumSykdomsgradVurdertMessage

internal class MinimumSykdomsgradVurdertRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "minimum_sykdomsgrad_vurdert"
    override val riverName = "minimum_sykdomsgrad_vurdert"

    override fun validate(message: JsonMessage) {
        message.requireKey("@id", "fødselsnummer")
        message.requireArray("perioderMedMinimumSykdomsgradVurdertOk") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("perioderMedMinimumSykdomsgradVurdertIkkeOk") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
    }

    override fun createMessage(packet: JsonMessage) = MinimumSykdomsgradVurdertMessage(
        packet, Meldingsporing(
        id = packet["@id"].asText().toUUID(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}

