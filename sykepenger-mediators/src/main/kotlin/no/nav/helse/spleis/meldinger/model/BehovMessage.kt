package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spleis.Meldingsporing

// Understands a JSON message representing a Need with solution
internal sealed class BehovMessage(packet: JsonMessage) : HendelseMessage(packet) {
    override fun additionalTracinginfo(packet: JsonMessage): Map<String, Any> {
        return mapOf(
            "behov" to packet["@behov"].map(JsonNode::asText)
        )
    }
}
