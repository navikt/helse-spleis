package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
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
        message.requireKey("vedtaksperiodeId", "tilstand", "utbetalingId")
        message.requireKey("@løsning.${Simulering.name}.status")
        message.requireKey("Simulering.fagsystemId")
        message.requireKey("Simulering.fagområde")
        message.require("@løsning.${Simulering.name}") { løsning ->
            message.require("@løsning.${Simulering.name}.status") {
                SimuleringMessage.Simuleringstatus.valueOf(it.asText())
            }
            if (løsning["status"].asText() == "OK") {
                message.interestedIn("@løsning.${Simulering.name}.simulering")
                message.forbid("@løsning.${Simulering.name}.feilmelding")
            } else {
                message.forbid("@løsning.${Simulering.name}.simulering")
                message.requireKey("@løsning.${Simulering.name}.feilmelding")
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = SimuleringMessage(
        packet, Meldingsporing(
        id = packet["@id"].asText().toUUID(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
