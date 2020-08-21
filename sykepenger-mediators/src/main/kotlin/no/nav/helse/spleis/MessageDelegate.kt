package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage

interface MessageDelegate {
    operator fun get(key: String): JsonNode
    fun toJson(): String
}

internal class JsonMessageDelegate(private val jsonMessage: JsonMessage) : MessageDelegate {
    override fun get(key: String): JsonNode {
        return jsonMessage[key]
    }

    override fun toJson(): String {
        return jsonMessage.toJson()
    }
}

internal class JsonNodeDelegate(private val jsonNode: JsonNode): MessageDelegate {
    override fun get(key: String): JsonNode {
        if (!key.contains(".")) return jsonNode.path(key)
        return key.split(".").fold(jsonNode) { result, keyPart ->
            result.path(keyPart)
        }
    }

    override fun toJson(): String {
        return jsonNode.toString()
    }

}
