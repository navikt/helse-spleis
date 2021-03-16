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

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId", "tilstand", "utbetalingId")
        message.requireKey("@løsning.${Godkjenning.name}.godkjent")
        message.requireKey("@løsning.${Godkjenning.name}.saksbehandlerIdent")
        message.requireKey("@løsning.${Godkjenning.name}.saksbehandlerEpost")
        message.require("@løsning.${Godkjenning.name}.godkjenttidspunkt", JsonNode::asLocalDateTime)
        message.requireKey("@løsning.${Godkjenning.name}.automatiskBehandling")
        message.requireKey("@løsning.${Godkjenning.name}.makstidOppnådd")
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingsgodkjenningMessage(packet)
}
