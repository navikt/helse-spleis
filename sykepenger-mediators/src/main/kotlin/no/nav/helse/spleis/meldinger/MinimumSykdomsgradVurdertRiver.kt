package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.MinimumSykdomsgradVurdertMessage

internal class MinimumSykdomsgradVurdertRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "minimum_sykdomsgrad_vurdert"
    override val riverName = "minimum_sykdomsgrad_vurdert"

    override fun validate(message: JsonMessage) {
        message.requireKey("@id", "fødselsnummer", "aktørId")
        message.requireArray("har_tapt_tilstrekkelig_arbeidstid") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("har_ikke_tapt_tilstrekkelig_arbeidstid") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
    }

    override fun createMessage(packet: JsonMessage) = MinimumSykdomsgradVurdertMessage(packet)
}

