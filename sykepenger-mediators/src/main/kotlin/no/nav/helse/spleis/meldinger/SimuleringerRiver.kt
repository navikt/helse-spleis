package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.Behov.Behovstype.Simulering
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.SimuleringMessage

internal class SimuleringerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Simulering)
    override val riverName = "Simulering"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId", "utbetalingId")
        message.requireKey("@løsning.${Simulering.utgåendeNavn}.status")
        message.requireKey("Simulering.fagsystemId")
        message.requireKey("Simulering.fagområde")
        message.require("@løsning.${Simulering.utgåendeNavn}") { løsning ->
            message.require("@løsning.${Simulering.utgåendeNavn}.status") {
                SimuleringMessage.Simuleringstatus.valueOf(it.asText())
            }
            if (løsning["status"].asText() == "OK") {
                message.interestedIn("@løsning.${Simulering.utgåendeNavn}.simulering")
                message.forbid("@løsning.${Simulering.utgåendeNavn}.feilmelding")
            } else {
                message.forbid("@løsning.${Simulering.utgåendeNavn}.simulering")
                message.requireKey("@løsning.${Simulering.utgåendeNavn}.feilmelding")
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = SimuleringMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
