package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

fun JsonNodeSerde(objectMapper: ObjectMapper) =
        Serdes.serdeFrom(JsonNodeSerializer(objectMapper), JsonNodeDeserializer(objectMapper))

class JsonNodeDeserializer(private val objectMapper: ObjectMapper): Deserializer<JsonNode> {
    private val log = LoggerFactory.getLogger(JsonNodeDeserializer::class.java)

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) { }

    override fun deserialize(topic: String?, data: ByteArray?): JsonNode? {
        return data?.let {
            try {
                objectMapper.readTree(it)
            } catch (e: Exception) {
                log.warn("Not a valid json",e)
                null
            }
        }
    }

    override fun close() { }
}

class JsonNodeSerializer(private val objectMapper: ObjectMapper): Serializer<JsonNode> {
    private val log = LoggerFactory.getLogger(JsonNodeSerializer::class.java)

    override fun serialize(topic: String?, data: JsonNode?): ByteArray? {
        return data?.let {
            try {
                objectMapper.writeValueAsBytes(it)
            } catch (e: Exception) {
                log.warn("Could not serialize JsonNode",e)
                null
            }
        }
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) { }
    override fun close() { }

}
