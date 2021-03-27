package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(packet: JsonMessage) : HendelseMessage(packet) {
    override val fødselsnummer = packet["fødselsnummer"].asText()

    override fun additionalTracinginfo(packet: JsonMessage): Map<String, Any> {
        return mapOf(
            "behov" to packet["@behov"].map(JsonNode::asText)
        )
    }
}
