package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ProblemerTest {

    private lateinit var problemer: Problemer

    private val Message = "the message"

    @BeforeEach
    internal fun setUp() {
        problemer = Problemer(Message)
    }

    @Test
    internal fun `inneholder original melding`() {
        assertFalse(problemer.toString().contains(Message))
        problemer.info("info message")
        assertTrue(problemer.toString().contains(Message))
    }

    @Test
    internal fun `har ingen feil ved default`() {
        assertFalse(problemer.hasErrors())
    }

    @Test
    internal fun `severe oppdaget og kaster exception`() {
        assertThrows<Problemer> { problemer.severe("Severe error") }
        assertTrue(problemer.hasErrors())
        assertTrue(problemer.toString().contains("Severe error"))
    }

    @Test
    internal fun `error oppdaget`() {
        problemer.error("Error")
        assertTrue(problemer.hasErrors())
        assertTrue(problemer.toString().contains("Error"))
    }

    @Test
    internal fun `warning oppdaget`() {
        problemer.warn("Warning explanation")
        assertFalse(problemer.hasErrors())
        assertTrue(problemer.toString().contains("Warning explanation"))
    }
}
