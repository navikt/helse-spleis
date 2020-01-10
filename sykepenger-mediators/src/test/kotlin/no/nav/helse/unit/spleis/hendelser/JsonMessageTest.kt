package no.nav.helse.unit.spleis.hendelser

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageProblems
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class JsonMessageTest {

    private val ValidJson = "{\"foo\": \"bar\"}"
    private val InvalidJson = "foo"

    @Test
    internal fun `invalid json`() {
        val problems = MessageProblems(InvalidJson)
        JsonMessage(InvalidJson, problems)
        assertTrue(problems.hasErrors())
    }

    @Test
    internal fun `valid json`() {
        val problems = MessageProblems(ValidJson)
        JsonMessage(ValidJson, problems)
        assertFalse(problems.hasErrors())
    }

    @Test
    internal fun `extended message`() {
        "not_valid_json".also { json ->
            val problems = MessageProblems(json)
            ExtendedMessage(json, problems)
            assertTrue(problems.hasErrors())
        }

        "{}".also { json ->
            val problems = MessageProblems(json)
            ExtendedMessage(json, problems)
            assertTrue(problems.hasErrors())
        }

        "{\"required_key\": \"foo\"}".also { json ->
            val problems = MessageProblems(json)
            ExtendedMessage(json, problems)
            assertFalse(problems.hasErrors())
        }
    }

    class ExtendedMessage(originalMessage: String, problems: MessageProblems) : JsonMessage(originalMessage, problems) {
        init {
            requiredKey("required_key")
        }
    }
}
