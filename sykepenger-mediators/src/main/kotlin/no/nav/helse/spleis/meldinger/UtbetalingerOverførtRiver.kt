package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingOverførtMessage

internal class UtbetalingerOverførtRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Utbetaling)
    override val riverName = "Utbetaling overført"

    override fun validate(message: JsonMessage) {
        message.demandValue("@løsning.${Utbetaling.name}.status", Oppdragstatus.OVERFØRT.name)
        message.requireKey("@løsning.${Utbetaling.name}.avstemmingsnøkkel")
        message.require("@løsning.${Utbetaling.name}.overføringstidspunkt", JsonNode::asLocalDateTime)
        message.requireKey("${Utbetaling.name}.fagsystemId", "utbetalingId")
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingOverførtMessage(packet)
}
