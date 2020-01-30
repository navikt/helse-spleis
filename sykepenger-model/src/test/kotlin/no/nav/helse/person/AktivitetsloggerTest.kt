package no.nav.helse.person

import org.junit.jupiter.api.Assertions.*
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
        val infomelding = "info message"
        aktivitetslogger.info(infomelding)
        assertTrue(aktivitetslogger.toReport().contains(Message))
        assertInfo(infomelding)
    }

    @Test
    internal fun `har ingen feil ved default`() {
        assertFalse(aktivitetslogger.hasErrors())
    }

    @Test
    internal fun `severe oppdaget og kaster exception`() {
        val melding = "Severe error"
        assertThrows<Aktivitetslogger.AktivitetException> { aktivitetslogger.severe(melding) }
        assertTrue(aktivitetslogger.hasErrors())
        assertTrue(aktivitetslogger.toString().contains(melding))
        assertSevere(melding)
    }

    @Test
    internal fun `error oppdaget`() {
        val melding = "Error"
        aktivitetslogger.error(melding)
        assertTrue(aktivitetslogger.hasErrors())
        assertTrue(aktivitetslogger.toString().contains(melding))
        assertError(melding)
    }

    @Test
    internal fun `warning oppdaget`() {
        val melding = "Warning explanation"
        aktivitetslogger.warn(melding)
        assertFalse(aktivitetslogger.hasErrors())
        assertTrue(aktivitetslogger.toString().contains(melding))
        assertWarn(melding)
    }

    private fun assertInfo(message: String) {
        var visitorCalled = false
        aktivitetslogger.accept(object : AktivitetsloggerVisitor {
            override fun visitInfo(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertWarn(message: String) {
        var visitorCalled = false
        aktivitetslogger.accept(object : AktivitetsloggerVisitor {
            override fun visitWarn(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertError(message: String) {
        var visitorCalled = false
        aktivitetslogger.accept(object : AktivitetsloggerVisitor {
            override fun visitError(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertSevere(message: String) {
        var visitorCalled = false
        aktivitetslogger.accept(object : AktivitetsloggerVisitor {
            override fun visitSevere(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }
}
