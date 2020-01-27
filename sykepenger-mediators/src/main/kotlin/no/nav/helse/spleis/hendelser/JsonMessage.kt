package no.nav.helse.spleis.hendelser


import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

// Understands a specific JSON-formatted message
// Implements GoF visitor pattern to enable working on the specific types
internal open class JsonMessage(private val originalMessage: String, private val problems: Aktivitetslogger) {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val json: JsonNode
    private val recognizedKeys = mutableMapOf<String, JsonNode>()

    private val nestedKeySeparator = '.'

    init {
        json = try {
            objectMapper.readTree(originalMessage)
        } catch (err: JsonParseException) {
            problems.severe("Invalid JSON per Jackson library: ${err.message}")
            objectMapper.nullNode()
        }
    }

    open fun accept(processor: MessageProcessor) {}

    fun requiredKey(vararg keys: String) {
        keys.forEach { requiredKey(it) }
    }

    fun requiredKey(key: String) {
        if (isKeyMissing(key)) return problems.error("Missing required key $key")
        if (node(key).isNull) return problems.error("Missing required key $key; value is null")
        accessor(key)
    }

    fun requiredValue(key: String, value: Boolean) {
        if (isKeyMissing(key) || !node(key).isBoolean || node(key).booleanValue() != value) {
            return problems.error("Required $key is not boolean $value")
        }
        accessor(key)
    }

    fun requiredValue(key: String, value: String) {
        if (isKeyMissing(key) || !node(key).isTextual || node(key).asText() != value) {
            return problems.error("Required $key is not string $value")
        }
        accessor(key)
    }

    fun requiredValueOneOf(key: String, values: List<String>) {
        if (isKeyMissing(key) || !node(key).isTextual || node(key).asText() !in values) {
            return problems.error("Required $key must be one of $values")
        }
        accessor(key)
    }

    fun requiredValues(key: String, values: List<String>) {
        if (isKeyMissing(key) || !node(key).isArray || !node(key).map(JsonNode::asText).containsAll(values)) {
            return problems.error("Required $key does not contains $values")
        }
        accessor(key)
    }

    fun requiredValues(key: String, vararg values: Enum<*>) {
        requiredValues(key, values.map(Enum<*>::name))
    }

    fun interestedIn(key: String) {
        accessor(key)
    }

    private fun accessor(key: String) {
        recognizedKeys.computeIfAbsent(key) { node(key) }
    }

    private fun isKeyMissing(key: String) = node(key).isMissingNode

    private fun node(path: String): JsonNode {
        if (!path.contains(nestedKeySeparator)) return json.path(path)
        return path.split(nestedKeySeparator).fold(json) { result, key ->
            result.path(key)
        }
    }

    operator fun get(key: String): JsonNode =
        requireNotNull(recognizedKeys[key]) { "$key is unknown; keys must be declared as required, forbidden, or interesting" }

    fun toJson() = originalMessage
}

internal fun JsonNode.asLocalDate() =
    asText().let { LocalDate.parse(it) }

internal fun JsonNode.asYearMonth() =
    asText().let { YearMonth.parse(it) }

internal fun JsonNode.asOptionalLocalDate() =
    takeIf(JsonNode::isTextual)?.asText()?.takeIf(String::isNotEmpty)?.let { LocalDate.parse(it) }

internal fun JsonNode.asLocalDateTime() =
    asText().let { LocalDateTime.parse(it) }

internal fun asPeriode(jsonNode: JsonNode) =
    Periode(jsonNode.path("fom").asLocalDate(), jsonNode.path("tom").asLocalDate())
