package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.hendelser.model.NySøknadMessage

internal class NyeSøknader(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "ny_søknad"
    override val riverName = "Ny søknad"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("sykmeldingId")
        packet.requireValue("status", "NY")
        packet.require("opprettet", JsonNode::asLocalDateTime)
    }

    override fun createMessage(packet: JsonMessage) = NySøknadMessage(packet)
}
