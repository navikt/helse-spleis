package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

// Understands a specific JSON-formatted message
internal open class JsonMessage(private val originalMessage: String, private val problems: MessageProblems) {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val json: JsonNode

    init {
        json = try {
            objectMapper.readTree(originalMessage)
        } catch (err: JsonParseException) {
            problems.fatalError("Invalid JSON per Jackson library: ${err.message}")
            objectMapper.nullNode()
        }
    }

    fun requiredKey(vararg keys: String) {
        keys.forEach { requiredKey(it) }
    }

    fun requiredKey(key: String) {
        if (json.path(key).isMissingNode) {
            problems.error("Missing required key $key")
        } else if (json[key].isNull) {
            problems.error("Missing required key $key; value is null")
        }
    }

    fun requiredValue(key: String, value: Boolean) {
        if (!json.path(key).isBoolean || json.path(key).booleanValue() != value) {
            problems.error("Required $key is not boolean $value")
        }
    }

    fun toJson() = originalMessage
}
