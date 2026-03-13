package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

// Understands a JSON message representing a Need with solution
internal sealed class BehovMessage(packet: JsonMessage) : HendelseMessage(packet) {
    override fun additionalTracinginfo(packet: JsonMessage): Map<String, Any> {
        return mapOf(
            "behov" to packet["@behov"].map(JsonNode::asText)
        )
    }

    protected fun <T> JsonMessage.mapFraArrayEllerObjectMedArray(key: String, arraynavn: String, mapEntry: (jsonNode: JsonNode) -> T) =
        getArray(key, arraynavn).map { mapEntry(it) }

    protected fun JsonMessage.getArray(key: String, arraynavn: String): JsonNode {
        return when (val node = get(key)) {
            is ArrayNode -> node
            is ObjectNode -> node.path(arraynavn)
            is MissingNode,
            is NullNode -> jacksonObjectMapper().createArrayNode()
            else -> error("Det var jo merkelig å se en ${node::class::simpleName} her!")
        }
    }
}
