package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingOverførtMessage

internal class UtbetalingerOverførtRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Utbetaling)
    override val riverName = "Utbetaling"

    override fun validate(packet: JsonMessage) {
        packet.demandValue("@løsning.${Utbetaling.name}.status", Oppdragstatus.OVERFØRT.name)
        packet.requireKey("@løsning.${Utbetaling.name}.overføringstidspunkt")
        packet.require("@løsning.${Utbetaling.name}.avstemmingsnøkkel", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingOverførtMessage(packet)
}
