package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

// Understands a specific JSON-formatted message
// Implements GoF visitor pattern to enable working on the specific types
internal open class JsonMessage(private val originalMessage: String, private val problems: MessageProblems) {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val json: JsonNode
    private val recognizedKeys = mutableMapOf<String, JsonNode>()

    init {
        json = try {
            objectMapper.readTree(originalMessage)
        } catch (err: JsonParseException) {
            problems.fatalError("Invalid JSON per Jackson library: ${err.message}")
            objectMapper.nullNode()
        }
    }

    open fun accept(processor: MessageProcessor) {}

    fun requiredKey(vararg keys: String) {
        keys.forEach { requiredKey(it) }
    }

    fun requiredKey(key: String) {
        if (isKeyMissing(key)) return problems.error("Missing required key $key")
        if (json.path(key).isNull) return problems.error("Missing required key $key; value is null")
        accessor(key)
    }

    fun requiredValue(key: String, value: Boolean) {
        if (isKeyMissing(key) || !json.path(key).isBoolean || json.path(key).booleanValue() != value) {
            return problems.error("Required $key is not boolean $value")
        }
        accessor(key)
    }

    fun requiredValue(key: String, value: String) {
        if (isKeyMissing(key) || !json.path(key).isTextual || json.path(key).asText() != value) {
            return problems.error("Required $key is not string $value")
        }
        accessor(key)
    }

    fun requiredValues(key: String, values: List<String>) {
        if (isKeyMissing(key) || !json.path(key).isArray || !values.all { it in (json.path(key) as ArrayNode).map { node -> node.textValue()} }) {
            return problems.error("Required $key does not contains $values")
        }
        accessor(key)
    }

    fun requiredValues(key: String, vararg values: Enum<*>) {
        requiredValues(key, values.map { it.name })
    }

    fun interestedIn(key: String) {
        accessor(key)
    }

    private fun accessor(key: String) {
        recognizedKeys.computeIfAbsent(key) { json.path(key) }
    }

    private fun isKeyMissing(key: String) = json.path(key).isMissingNode

    operator fun get(key: String): JsonNode =
        requireNotNull(recognizedKeys[key]) { "$key is unknown; keys must be declared as required, forbidden, or interesting" }

    fun toJson() = originalMessage
}
