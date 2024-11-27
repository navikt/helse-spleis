package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.UtbetalingMessage
import no.nav.helse.utbetalingslinjer.Oppdragstatus

internal class UtbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Utbetaling)
    override val riverName = "Utbetaling"

    private val gyldigeStatuser = Oppdragstatus.entries.map(Enum<*>::name)

    init {
        river.precondition { message ->
            message.forbidValue("@løsning.${Utbetaling.name}.status", "MOTTATT")
        }
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("@løsning.${Utbetaling.name}")
        message.requireAny("@løsning.${Utbetaling.name}.status", gyldigeStatuser)
        message.requireKey("${Utbetaling.name}.fagsystemId", "utbetalingId", "@løsning.${Utbetaling.name}.beskrivelse")
        message.requireKey("@løsning.${Utbetaling.name}.avstemmingsnøkkel")
        message.require("@løsning.${Utbetaling.name}.overføringstidspunkt", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingMessage(
        packet, Meldingsporing(
        id = packet["@id"].asText().toUUID(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
