package no.nav.helse.rapids_rivers

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RiverTest {

    @Test
    internal fun `invalid json`() {
        river.onMessage("invalid json", context)
        assertFalse(gotMessage)
    }

    @Test
    internal fun `no validations`() {
        river.onMessage("{}", context)
        assertTrue(gotMessage)
    }

    @Test
    internal fun `failed validations`() {
        river.validate { false }
        river.onMessage("{}", context)
        assertFalse(gotMessage)
    }

    @Test
    internal fun `passing validations`() {
        river.validate { it.path("hello").asText() == "world" }
        river.onMessage("{\"hello\": \"world\"}", context)
        assertTrue(gotMessage)
    }

    private val context = object : RapidsConnection.MessageContext {
        override fun send(message: String) {}

        override fun send(key: String, message: String) {}
    }

    private var gotMessage = false
    private lateinit var river: River

    @BeforeEach
    internal fun setup() {
        river = River().apply {
            register(object : River.PacketListener {
                override fun onPacket(packet: JsonNode, context: RapidsConnection.MessageContext) {
                    gotMessage = true
                }
            })
        }
    }
}
