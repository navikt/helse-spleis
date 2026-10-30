package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.serde.spleisJsonMapperBuilder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.MissingNode
import tools.jackson.databind.node.NullNode
import tools.jackson.databind.node.ObjectNode

// Understands a JSON message representing a Need with solution
internal sealed class BehovMessage(
    packet: JsonMessage,
) : HendelseMessage(packet) {
    override fun additionalTracinginfo(packet: JsonMessage): Map<String, Any> =
        mapOf(
            "behov" to packet["@behov"].values().map(JsonNode::asText),
        )

    protected fun <T> JsonMessage.mapFraArrayEllerObjectMedArray(
        key: String,
        arraynavn: String,
        mapEntry: (jsonNode: JsonNode) -> T,
    ) = getArray(key, arraynavn).values().map { mapEntry(it) }

    protected fun JsonMessage.getArray(
        key: String,
        arraynavn: String,
    ): JsonNode =
        when (val node = get(key)) {
            is ArrayNode -> node
            is ObjectNode -> node.path(arraynavn)
            is MissingNode,
            is NullNode,
            -> spleisJsonMapperBuilder().build().createArrayNode()
            else -> error("Det var jo merkelig å se en ${node::class::simpleName} her!")
        }
}
