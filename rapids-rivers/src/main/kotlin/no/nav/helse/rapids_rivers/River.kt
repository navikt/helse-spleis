package no.nav.helse.rapids_rivers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

open class River : RapidsConnection.MessageListener {

    private val validations = mutableListOf<(JsonNode) -> Boolean>()
    private val listeners = mutableListOf<PacketListener>()

    fun validate(validation: (JsonNode) -> Boolean) {
        validations.add(validation)
    }

    fun register(listener: PacketListener) {
        listeners.add(listener)
    }

    override fun onMessage(message: String, context: RapidsConnection.MessageContext) {
        val packet = message.parseJson() ?: return
        for (v in validations) if (!v(packet)) return
        onPacket(packet, context)
    }

    private fun onPacket(packet: JsonNode, context: RapidsConnection.MessageContext) {
        listeners.forEach { it.onPacket(packet, context) }
    }

    interface PacketListener {
        fun onPacket(packet: JsonNode, context: RapidsConnection.MessageContext)
    }
}

private val objectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())

private fun String.parseJson() = try {
    objectMapper.readTree(this)
} catch (err: JsonProcessingException) {
    null
}

fun JsonNode.toJson() = objectMapper.writeValueAsString(this)
fun JsonNode.put(key: String, value: String) = (this as ObjectNode).put(key, value)
