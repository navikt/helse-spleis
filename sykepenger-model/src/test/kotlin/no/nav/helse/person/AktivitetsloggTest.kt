package no.nav.helse.person

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AktivitetsloggTest {

    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var forelder: TestLogger

    @BeforeEach
    internal fun setUp() {
        forelder = TestLogger("Forelder")
        aktivitetslogg = forelder.logg
    }

    @Test
    internal fun `inneholder original melding`() {
        val infomelding = "info message"
        aktivitetslogg.info(infomelding)
        assertInfo(infomelding)
    }

    @Test
    internal fun `har ingen feil ved default`() {
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    internal fun `severe oppdaget og kaster exception`() {
        val melding = "Severe error"
        assertThrows<Aktivitetslogg.AktivitetException> { aktivitetslogg.severe(melding) }
        assertTrue(aktivitetslogg.hasErrors())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertSevere(melding)
    }

    @Test
    internal fun `error oppdaget`() {
        val melding = "Error"
        aktivitetslogg.error(melding)
        assertTrue(aktivitetslogg.hasErrors())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertError(melding)
    }

    @Test
    internal fun `warning oppdaget`() {
        val melding = "Warning explanation"
        aktivitetslogg.warn(melding)
        assertFalse(aktivitetslogg.hasErrors())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertWarn(melding)
    }

    @Test
    internal fun `Melding sendt til forelder`(){
        val barn = TestLogger("Barn", forelder.logg.barn())
        "info message".also {
            barn.info(it)
            assertInfo(it, barn.logg)
            assertInfo(it, forelder.logg)
        }
        "error message".also {
            barn.error(it)
            assertError(it, barn.logg)
            assertError(it, forelder.logg)
        }
    }

    @Test
    internal fun `Melding sendt fra barnebarn til forelder`(){
        val hendelse = TestLogger("Hendelse", forelder.logg.barn())
        val barn = TestLogger("Barn 1", forelder.logg.barn())
        hendelse.logg.forelder(barn.logg)
        val barnebarn = TestLogger("Barnebarn 1", barn.logg.barn())
        hendelse.logg.forelder(barnebarn.logg)
        "info message".also {
            hendelse.info(it)
            assertInfo(it, hendelse.logg)
            assertInfo(it, barnebarn.logg)
            assertInfo(it, barn.logg)
            assertInfo(it, forelder.logg)
        }
        "error message".also {
            hendelse.error(it)
            assertError(it, hendelse.logg)
            assertError(it, barnebarn.logg)
            assertError(it, barn.logg)
            assertError(it, forelder.logg)
            assertError("Hendelse", forelder.logg)
            assertError("Barnebarn 1", forelder.logg)
            assertError("Barn 1", forelder.logg)
            println(forelder.logg)
        }
    }

    private fun assertInfo(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitInfo(aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertWarn(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitWarn(aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertError(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitError(aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertTrue(message in aktivitet.toString(), aktivitetslogg.toString())
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertSevere(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitSevere(aktivitet: Aktivitetslogg.Aktivitet.Severe, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }


    private class TestLogger(private val melding: String, internal val logg: Aktivitetslogg = Aktivitetslogg()):
        Aktivitetsmelding, IAktivitetslogg by logg {

        init {
            logg.aktivitetsmelding(this)
        }

        override fun melding() = melding

    }
}
