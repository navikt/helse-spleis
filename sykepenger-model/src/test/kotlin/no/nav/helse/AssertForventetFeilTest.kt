package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class AssertForventetFeilTest {

    @Test
    fun `ønsket oppførsel er oppfylt`() {
        val assertionError = assertThrows<AssertionError> {
            assertForventetFeil(
                nå = { throw IllegalStateException("Irrelevant om det feiler med assertion eller exception") },
                ønsket = { assertTrue(true) }
            )
        }
        assertEquals("✅ Testen oppfører seg nå som ønsket! Fjern bruken av 'assertForventetFeil', og behold kun assertions for ønsket oppførsel ✅", assertionError.message)
    }

    @Test
    fun `nå-oppførsel er endret uten at det er ønsket oppførsel`() {
        val assertionError = assertThrows<AssertionError> {
            assertForventetFeil(
                nå = { assertTrue(false) },
                ønsket = { assertTrue(false) }
            )
        }
        assertEquals("⚠️ Testen har endret nå-oppførsel, men ikke til ønsket oppførsel ⚠️️️", assertionError.message)
    }

    @Test
    fun `nå-oppførsel inneholder utviklerfeil uten at det er ønsket oppførsel`() {
        val assertionError = assertThrows<AssertionError> {
            assertForventetFeil(
                nå = { throw IllegalStateException("I can cout to potato") },
                ønsket = { assertTrue(false) }
            )
        }
        assertEquals("☠️️ Feil i testkoden, feiler ikke på assertions ☠️️", assertionError.message)
    }

    @Test
    fun `både nå- og ønsket oppførsel er oppfylt`() {
        val assertionError = assertThrows<AssertionError> {
            assertForventetFeil(
                nå = { assertTrue(true) },
                ønsket = { assertTrue(true) }
            )
        }
        assertEquals("✅ Testen oppfører seg nå som ønsket! Fjern bruken av 'assertForventetFeil', og behold kun assertions for ønsket oppførsel ✅ ==> Expected java.lang.Throwable to be thrown, but nothing was thrown.", assertionError.message)
    }

    @Test
    fun `nå-oppførsel er oppfylt uten at det er ønsket oppførsel`() {
        assertDoesNotThrow {
            assertForventetFeil(
                nå = { assertTrue(true) },
                ønsket = { assertTrue(false) }
            )
        }
    }

    @Test
    fun `ønsket oppførsel inneholder utviklerfeil`() {
        val assertionError = assertThrows<AssertionError> {
            assertForventetFeil(
                nå = { assertTrue(true) },
                ønsket = { throw IllegalStateException("I can cout to potato") }
            )
        }
        assertEquals("☠️️ Feil i testkoden, feiler ikke på assertions ☠️️", assertionError.message)
    }
}
