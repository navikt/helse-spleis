package no.nav.helse.unit.spleis.hendelser

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.Parser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ParserTest : Parser.ParserDirector {

    @Test
    internal fun `message is kept in the problems object`() {
        val message = "{\"key\": \"value\"}"
        parser.onMessage(message)
        assertTrue(unrecognizedMessage)
        assertContains(message, messageProblems)
    }

    @Test
    internal fun `when message is not recognized, errors are accumulated`() {
        messageFactory("key1_not_set")
        messageFactory("key2_not_set")
        parser.onMessage("{\"key\": \"value\"}")

        assertTrue(unrecognizedMessage)
        assertTrue(messageProblems.hasErrors())
        assertContains("key1_not_set", messageProblems)
        assertContains("key2_not_set", messageProblems)
    }

    @Test
    internal fun `when message is recognized, errors are not accumulated`() {
        messageFactory("key1_not_set")
        var message2: JsonMessage? = null
        messageFactory {
            requiredKey("key")
            message2 = this
        }
        parser.onMessage("{\"key\": \"value\"}")

        assertEquals(message2, recognizedMessage)
        assertFalse(messageProblems.hasErrors())
        assertNotContains("key1_not_set", messageProblems)
    }

    @Test
    internal fun `stops at first recognizer without error`() {
        var message1: JsonMessage? = null
        messageFactory {
            requiredKey("key")
            message1 = this
        }
        messageFactory("key")

        parser.onMessage("{\"key\": \"value\"}")

        assertEquals(message1, recognizedMessage)
    }

    private fun assertContains(message: String, problems: MessageProblems) {
        assertTrue(problems.toString().contains(message))
    }

    private fun assertNotContains(message: String, problems: MessageProblems) {
        assertFalse(problems.toString().contains(message))
    }

    private lateinit var parser: Parser
    private var unrecognizedMessage = false
    private var recognizedMessage: JsonMessage? = null
    private lateinit var messageProblems: MessageProblems

    @BeforeEach
    internal fun setup() {
        parser = Parser(this)
        recognizedMessage = null
        unrecognizedMessage = false
    }

    override fun onRecognizedMessage(message: JsonMessage, warnings: MessageProblems) {
        recognizedMessage = message
        messageProblems = warnings
    }

    override fun onUnrecognizedMessage(problems: MessageProblems) {
        unrecognizedMessage = true
        messageProblems = problems
    }

    private fun messageFactory(requiredKey: String) {
        messageFactory {
            requiredKey(requiredKey)
        }
    }

    private fun messageFactory(block: JsonMessage.() -> Unit) {
        parser.register(object : MessageFactory<JsonMessage> {
            override fun createMessage(message: String, problems: MessageProblems): JsonMessage {
                return JsonMessage(message, problems).apply {
                    block(this)
                }
            }
        })
    }
}
