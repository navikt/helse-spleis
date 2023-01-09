package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(packet: JsonMessage, private val builder: NySøknadBuilder = NySøknadBuilder()) : SøknadMessage(packet, builder) {

    override fun _behandle(mediator: IHendelseMediator, packet: JsonMessage, context: MessageContext) {
        builder.fremtidigSøknad(packet["fremtidig_søknad"].asBoolean())
        mediator.behandle(this, builder.build(), context, packet["historiskeFolkeregisteridenter"].map(JsonNode::asText))
    }
}
