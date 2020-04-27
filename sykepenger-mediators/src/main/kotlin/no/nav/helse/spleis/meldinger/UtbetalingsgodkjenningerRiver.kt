package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingsgodkjenningMessage

internal class UtbetalingsgodkjenningerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Godkjenning)
    override val riverName = "Utbetalingsgodkjenning"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@løsning.${Godkjenning.name}.godkjent")
        packet.requireKey("@løsning.${Godkjenning.name}.saksbehandlerIdent")
        packet.require("@løsning.${Godkjenning.name}.godkjenttidspunkt", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingsgodkjenningMessage(packet)
}

internal class GammelUtbetalingsgodkjenningerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Godkjenning)
    override val riverName = "Gammel utbetalingsgodkjenning"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@løsning.${Godkjenning.name}.godkjent")
        packet.requireKey("saksbehandlerIdent")
        packet.require("godkjenttidspunkt", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingsgodkjenningMessage(packet)
}
