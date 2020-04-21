package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.junit.jupiter.api.fail

internal class TestRapid : RapidsConnection() {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val context = TestContext()
    private val messages = mutableListOf<Pair<String?, String>>()
    internal val inspektør get() = RapidInspektør(messages.toList())

    internal fun reset() {
        messages.clear()
    }

    fun sendTestMessage(message: String) {
        listeners.forEach { it.onMessage(message, context) }
    }

    override fun publish(message: String) {
        messages.add(null to message)
    }

    override fun publish(key: String, message: String) {
        messages.add(key to message)
    }

    override fun start() {}

    override fun stop() {}

    private inner class TestContext : MessageContext {
        override fun send(message: String) {
            publish(message)
        }

        override fun send(key: String, message: String) {
            publish(key, message)
        }
    }

    class RapidInspektør(private val messages: List<Pair<String?, String>>) {
        private val jsonmeldinger = mutableMapOf<Int, JsonNode>()

        fun events(name: String, onEach: (JsonNode) -> Unit) = messages.forEachIndexed { indeks, _ ->
            val message = melding(indeks)
            if (name == message.path("@event_name").asText()) onEach(message)
        }

        fun melding(indeks: Int) = jsonmeldinger.getOrPut(indeks) { objectMapper.readTree(messages[indeks].second) }
        fun id(indeks: Int) = felt(indeks, "@id").asText()
        fun løsning(indeks: Int, behov: String, block: (JsonNode) -> Unit = {}) = felt(indeks, "@løsning").path(behov).takeUnless(
            JsonNode::isMissingOrNull)?.also(block) ?: fail {
            "Behov har ikke løsning for $behov"
        }
        fun felt(indeks: Int, felt: String) = melding(indeks).path(felt).takeUnless(JsonNode::isMissingOrNull) ?: fail {
            "Melding mangler felt $felt"
        }
        fun antall() = messages.size
    }
}
