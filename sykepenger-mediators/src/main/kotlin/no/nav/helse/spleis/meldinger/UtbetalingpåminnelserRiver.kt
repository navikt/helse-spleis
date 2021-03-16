package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingpåminnelseMessage

internal class UtbetalingpåminnelserRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "utbetalingpåminnelse"
    override val riverName = "Utbetalingpåminnelse"

    override fun validate(message: JsonMessage) {
        message.requireKey("antallGangerPåminnet", "utbetalingId",
            "organisasjonsnummer", "fødselsnummer", "aktørId")
        message.require("endringstidspunkt", JsonNode::asLocalDateTime)
        message.requireAny("status", Utbetalingstatus.values().map(Enum<*>::name))
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingpåminnelseMessage(packet)
}
