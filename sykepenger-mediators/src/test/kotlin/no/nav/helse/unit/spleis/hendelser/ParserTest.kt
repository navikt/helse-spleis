package no.nav.helse.unit.spleis.hendelser

import no.nav.helse.spleis.hendelser.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
        recognizer(listOf("key1_not_set"))
        recognizer(listOf("key2_not_set"))
        parser.onMessage("{\"key\": \"value\"}")

        assertTrue(unrecognizedMessage)
        assertTrue(messageProblems.hasErrors())
        assertContains("key1_not_set", messageProblems)
        assertContains("key2_not_set", messageProblems)
    }

    @Test
    internal fun `when message is recognized, errors are not accumulated`() {
        val recognizer1 = recognizer(listOf("key1_not_set"))
        val recognizer2 = recognizer(listOf("key"))
        parser.onMessage("{\"key\": \"value\"}")

        assertFalse(unrecognizedMessage)
        assertFalse(recognizer1.recognizedMessage)
        assertTrue(recognizer2.recognizedMessage)

        assertFalse(recognizer2.messageWarnings.hasErrors())
        assertNotContains("key1_not_set", recognizer2.messageWarnings)
    }

    @Test
    internal fun `stops at first recognizer without error`() {
        val recognizer1 = recognizer(listOf("key"))
        val recognizer2 = recognizer(listOf("key"))

        parser.onMessage("{\"key\": \"value\"}")

        assertTrue(recognizer1.recognizedMessage)
        assertFalse(recognizer2.recognizedMessage)
    }

    private fun assertContains(message: String, problems: MessageProblems) {
        assertTrue(problems.toString().contains(message))
    }

    private fun assertNotContains(message: String, problems: MessageProblems) {
        assertFalse(problems.toString().contains(message))
    }

    private lateinit var parser: Parser
    private var unrecognizedMessage = false
    private lateinit var messageProblems: MessageProblems

    @BeforeEach
    internal fun setup() {
        parser = Parser(this)
        unrecognizedMessage = false
    }

    override fun onUnrecognizedMessage(problems: MessageProblems) {
        unrecognizedMessage = true
        messageProblems = problems
    }

    private fun recognizer(requiredKeys: List<String>): TestRecognizer.Director {
        val director = TestRecognizer.Director()
        TestRecognizer(director, requiredKeys).apply {
            parser.register(this)
        }
        return director
    }


    private class TestRecognizer(director: MessageDirector<JsonMessage>, private val keys: List<String>) : MessageRecognizer<JsonMessage>(director) {

        override fun createMessage(message: String, problems: MessageProblems): JsonMessage {
            return JsonMessage(message, problems).apply {
                keys.forEach { requiredKey(it) }
            }
        }

        class Director : MessageDirector<JsonMessage> {
            var recognizedMessage = false
            lateinit var messageWarnings: MessageProblems

            override fun onMessage(message: JsonMessage, warnings: MessageProblems) {
                recognizedMessage = true
                messageWarnings = warnings
            }
        }
    }
}
