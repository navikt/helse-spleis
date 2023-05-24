package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingMessage
import no.nav.helse.utbetalingslinjer.Oppdragstatus

internal class UtbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Utbetaling)
    override val riverName = "Utbetaling"

    private val gyldigeStatuser = Oppdragstatus.values().map(Enum<*>::name)

    override fun validate(message: JsonMessage) {
        message.requireKey("@løsning.${Utbetaling.name}")
        message.rejectValue("@løsning.${Utbetaling.name}.status", "MOTTATT")
        message.requireAny("@løsning.${Utbetaling.name}.status", gyldigeStatuser)
        message.requireKey("${Utbetaling.name}.fagsystemId", "utbetalingId", "@løsning.${Utbetaling.name}.beskrivelse")
        message.requireKey("@løsning.${Utbetaling.name}.avstemmingsnøkkel")
        message.require("@løsning.${Utbetaling.name}.overføringstidspunkt", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingMessage(packet)
}
