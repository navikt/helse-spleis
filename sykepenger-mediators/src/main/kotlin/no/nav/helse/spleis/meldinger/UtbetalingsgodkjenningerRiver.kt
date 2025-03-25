package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.UtbetalingsgodkjenningMessage

internal class UtbetalingsgodkjenningerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Godkjenning)
    override val riverName = "Utbetalingsgodkjenning"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId", "utbetalingId")
        message.requireKey("@løsning.${Godkjenning.name}.godkjent")
        message.requireKey("@løsning.${Godkjenning.name}.saksbehandlerIdent")
        message.requireKey("@løsning.${Godkjenning.name}.saksbehandlerEpost")
        message.require("@løsning.${Godkjenning.name}.godkjenttidspunkt", JsonNode::asLocalDateTime)
        message.requireKey("@løsning.${Godkjenning.name}.automatiskBehandling")
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingsgodkjenningMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
