package no.nav.helse.spleis.meldinger

import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SimuleringMessage

internal class SimuleringerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Simulering)
    override val riverName = "Simulering"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId", "tilstand")
        message.requireKey("@løsning.${Simulering.name}.status")
        message.require("@løsning.${Simulering.name}") { løsning ->
            message.require("@løsning.${Simulering.name}.status") {
                SimuleringMessage.Simuleringstatus.valueOf(it.asText())
            }
            if (løsning["status"].asText() == "OK") {
                message.requireKey("@løsning.${Simulering.name}.simulering")
                message.forbid("@løsning.${Simulering.name}.feilmelding")
            } else {
                message.forbid("@løsning.${Simulering.name}.simulering")
                message.requireKey("@løsning.${Simulering.name}.feilmelding")
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = SimuleringMessage(packet)
}
