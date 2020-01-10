package no.nav.helse.unit.spleis.hendelser

import no.nav.helse.spleis.hendelser.MessageProblems
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MessageProblemsTest {

    private lateinit var problems: MessageProblems

    private val Message = "the message"

    @BeforeEach
    internal fun setUp() {
        problems = MessageProblems(Message)
    }

    @Test
    internal fun containsMessage() {
        assertTrue(problems.toString().contains(Message))
    }

    @Test
    internal fun noProblemsFoundDefault() {
        assertFalse(problems.hasErrors())
    }

    @Test
    internal fun severeErrorsDetected() {
        problems.fatalError("Severe error")
        assertTrue(problems.hasErrors())
        assertTrue(problems.toString().contains("Severe error"))
    }

    @Test
    internal fun errorsDetected() {
        problems.error("Error")
        assertTrue(problems.hasErrors())
        assertTrue(problems.toString().contains("Error"))
    }

    @Test
    internal fun warningsDetected() {
        problems.warning("Warning explanation")
        assertFalse(problems.hasErrors())
        assertTrue(problems.toString().contains("Warning explanation"))
    }
}
