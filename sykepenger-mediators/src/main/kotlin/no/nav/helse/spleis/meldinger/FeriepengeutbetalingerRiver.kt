package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Feriepengeutbetaling
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
            message.forbidValue("@løsning.${Feriepengeutbetaling.name}.status", "MOTTATT")
        }
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("@løsning.${Feriepengeutbetaling.name}")
        message.requireAny("@løsning.${Feriepengeutbetaling.name}.status", gyldigeStatuser)
        message.requireKey("${Feriepengeutbetaling.name}.fagsystemId", "utbetalingId", "@løsning.${Feriepengeutbetaling.name}.beskrivelse")
        message.requireKey("@løsning.${Feriepengeutbetaling.name}.avstemmingsnøkkel")
        message.require("@løsning.${Feriepengeutbetaling.name}.overføringstidspunkt", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = FeriepengeutbetalingMessage(
        packet = packet,
        meldingsporing = Meldingsporing(id = packet.meldingsreferanseId(), fødselsnummer = packet["fødselsnummer"].asText())
    )
}
