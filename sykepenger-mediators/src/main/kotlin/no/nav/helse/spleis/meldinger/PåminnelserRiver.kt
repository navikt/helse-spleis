package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.time.LocalDateTime
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.PåminnelseMessage

internal class PåminnelserRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "påminnelse"
    override val riverName = "Påminnelse"

    init {
        river.precondition { packet -> packet.require("påminnelsestidspunkt") { require(it.asLocalDateTime() > LocalDateTime.now().minusHours(24)) } }
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("antallGangerPåminnet", "vedtaksperiodeId", "organisasjonsnummer", "fødselsnummer")
        message.require("tilstandsendringstidspunkt", JsonNode::asLocalDateTime)
        message.require("påminnelsestidspunkt", JsonNode::asLocalDateTime)
        message.require("nestePåminnelsestidspunkt", JsonNode::asLocalDateTime)
        message.requireAny("tilstand", TilstandType.entries.map(Enum<*>::name))
        message.interestedIn("ønskerReberegning", "ønskerInntektFraAOrdningen")
    }

    override fun createMessage(packet: JsonMessage) = PåminnelseMessage(
        packet,
        Meldingsporing(
            id = packet["@id"].asText().toUUID(),
            fødselsnummer = packet["fødselsnummer"].asText()
        )
    )
}
