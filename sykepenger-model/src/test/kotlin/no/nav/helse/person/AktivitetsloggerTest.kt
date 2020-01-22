package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AktivitetsloggerTest {

    private lateinit var aktivitetslogger: Aktivitetslogger

    private val Message = "the message"

    @BeforeEach
    internal fun setUp() {
        aktivitetslogger = Aktivitetslogger(Message)
    }

    @Test
    internal fun `inneholder original melding`() {
        assertFalse(aktivitetslogger.toString().contains(Message))
        aktivitetslogger.info("info message")
        assertTrue(aktivitetslogger.toString().contains(Message))
    }

    @Test
    internal fun `har ingen feil ved default`() {
        assertFalse(aktivitetslogger.hasErrors())
    }

    @Test
    internal fun `severe oppdaget og kaster exception`() {
        assertThrows<Aktivitetslogger.AktivitetException> { aktivitetslogger.severe("Severe error") }
        assertTrue(aktivitetslogger.hasErrors())
        assertTrue(aktivitetslogger.toString().contains("Severe error"))
    }

    @Test
    internal fun `error oppdaget`() {
        aktivitetslogger.error("Error")
        assertTrue(aktivitetslogger.hasErrors())
        assertTrue(aktivitetslogger.toString().contains("Error"))
    }

    @Test
    internal fun `warning oppdaget`() {
        aktivitetslogger.warn("Warning explanation")
        assertFalse(aktivitetslogger.hasErrors())
        assertTrue(aktivitetslogger.toString().contains("Warning explanation"))
    }
}
