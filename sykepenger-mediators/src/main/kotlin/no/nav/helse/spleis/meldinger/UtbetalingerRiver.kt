package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.Behov.Behovstype.Utbetaling
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
            message.forbidValue("@løsning.${Utbetaling.utgåendeNavn}.status", "MOTTATT")
        }
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("@løsning.${Utbetaling.utgåendeNavn}")
        message.requireAny("@løsning.${Utbetaling.utgåendeNavn}.status", gyldigeStatuser)
        message.requireKey("${Utbetaling.utgåendeNavn}.fagsystemId", "utbetalingId", "@løsning.${Utbetaling.utgåendeNavn}.beskrivelse")
        message.require("vedtaksperiodeId") { it.asText().toUUID() }
        message.require("behandlingId") { it.asText().toUUID() }
        message.requireKey("@løsning.${Utbetaling.utgåendeNavn}.avstemmingsnøkkel")
        message.require("@løsning.${Utbetaling.utgåendeNavn}.overføringstidspunkt", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
