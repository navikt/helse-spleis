package no.nav.helse.rapids_rivers

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RiverTest {

    @Test
    internal fun `invalid json`() {
        val river = TestRiver()
        river.onMessage("invalid json", context)
        assertFalse(river.gotMessage)
    }

    @Test
    internal fun `no validations`() {
        val river = TestRiver()
        river.onMessage("{}", context)
        assertTrue(river.gotMessage)
    }

    @Test
    internal fun `failed validations`() {
        val river = TestRiver()
        river.validate { false }
        river.onMessage("{}", context)
        assertFalse(river.gotMessage)
    }

    @Test
    internal fun `passing validations`() {
        val river = TestRiver()
        river.validate { it.path("hello").asText() == "world" }
        river.onMessage("{\"hello\": \"world\"}", context)
        assertTrue(river.gotMessage)
    }

    private val context = object : RapidsConnection.MessageContext {
        override fun send(message: String) {}

        override fun send(key: String, message: String) {}
    }

    private class TestRiver : River() {
        var gotMessage = false

        override fun onPacket(packet: JsonNode, context: RapidsConnection.MessageContext) {
            gotMessage = true
        }
    }
}
