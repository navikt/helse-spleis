package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.GjenopptaBehandlingMessage

internal class GjenopptaBehandlingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "gjenoppta_behandling"
    override val riverName = "Gjenoppta behandling"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer")
    }

    override fun createMessage(packet: JsonMessage) = GjenopptaBehandlingMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
