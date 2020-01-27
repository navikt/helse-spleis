package no.nav.helse.rapids_rivers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

abstract class River : RapidsConnection.MessageListener {

    private val validations = mutableListOf<(JsonNode) -> Boolean>()

    fun validate(validation: (JsonNode) -> Boolean) {
        validations.add(validation)
    }

    override fun onMessage(message: String, context: RapidsConnection.MessageContext) {
        val packet = message.parseJson() ?: return
        for (v in validations) if (!v(packet)) return
        onPacket(packet, context)
    }

    abstract fun onPacket(packet: JsonNode, context: RapidsConnection.MessageContext)
}

private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())

private fun String.parseJson() = try {
    objectMapper.readTree(this)
} catch (err: JsonProcessingException) {
    null
}

fun JsonNode.toJson() = objectMapper.writeValueAsString(this)
fun JsonNode.put(key: String, value: String) = (this as ObjectNode).put(key, value)
