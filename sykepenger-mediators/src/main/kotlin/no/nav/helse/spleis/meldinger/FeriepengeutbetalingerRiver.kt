package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.Behov.Behovstype.Feriepengeutbetaling
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.FeriepengeutbetalingMessage
import no.nav.helse.utbetalingslinjer.Oppdragstatus

internal class FeriepengeutbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Feriepengeutbetaling)
    override val riverName = "Feriepengeutbetaling"

    private val gyldigeStatuser = Oppdragstatus.entries.map(Enum<*>::name)

    init {
        river.precondition { message ->
            message.forbidValue("@løsning.${Feriepengeutbetaling.utgåendeNavn}.status", "MOTTATT")
        }
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("@løsning.${Feriepengeutbetaling.utgåendeNavn}")
        message.requireAny("@løsning.${Feriepengeutbetaling.utgåendeNavn}.status", gyldigeStatuser)
        message.requireKey("${Feriepengeutbetaling.utgåendeNavn}.fagsystemId", "utbetalingId", "@løsning.${Feriepengeutbetaling.utgåendeNavn}.beskrivelse")
        message.requireKey("@løsning.${Feriepengeutbetaling.utgåendeNavn}.avstemmingsnøkkel")
        message.require("@løsning.${Feriepengeutbetaling.utgåendeNavn}.overføringstidspunkt", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = FeriepengeutbetalingMessage(
        packet = packet,
        meldingsporing = Meldingsporing(id = packet.meldingsreferanseId(), fødselsnummer = packet["fødselsnummer"].asText())
    )
}
