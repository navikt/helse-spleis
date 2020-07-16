package no.nav.helse.spleis.meldinger

import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.SimuleringMessage

internal class SimuleringerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Simulering)
    override val riverName = "Simulering"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@løsning.${Simulering.name}.status")
        packet.require("@løsning.${Simulering.name}") { løsning ->
            packet.require("@løsning.${Simulering.name}.status") {
                SimuleringMessage.Simuleringstatus.valueOf(it.asText())
            }
            if (løsning["status"].asText() == "OK") {
                packet.requireKey("@løsning.${Simulering.name}.simulering")
                packet.forbid("@løsning.${Simulering.name}.feilmelding")
            } else {
                packet.forbid("@løsning.${Simulering.name}.simulering")
                packet.requireKey("@løsning.${Simulering.name}.feilmelding")
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = SimuleringMessage(JsonMessageDelegate(packet))
}
