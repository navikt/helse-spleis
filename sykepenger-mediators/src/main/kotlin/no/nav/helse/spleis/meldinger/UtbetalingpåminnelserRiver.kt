package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.UtbetalingpåminnelseMessage
import no.nav.helse.utbetalingslinjer.Utbetalingstatus

internal class UtbetalingpåminnelserRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "utbetalingpåminnelse"
    override val riverName = "Utbetalingpåminnelse"

    override fun validate(message: JsonMessage) {
        message.requireKey("antallGangerPåminnet", "utbetalingId", "organisasjonsnummer", "fødselsnummer")
        message.require("endringstidspunkt", JsonNode::asLocalDateTime)
        message.requireAny("status", Utbetalingstatus.entries.map(Enum<*>::name))
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingpåminnelseMessage(
        packet, Meldingsporing(
        id = packet["@id"].asText().toUUID(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
