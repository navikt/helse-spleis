package no.nav.helse

import no.nav.helse.Toggle.Companion.disable
import no.nav.helse.Toggle.Companion.enable
import no.nav.helse.Toggle.Companion.fraEnv
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ToggleTest {

    private lateinit var toggle: Toggle

    private fun prepareToggle(enabled: Boolean) =
        fraEnv("FINNES_IKKE", enabled).also { this.toggle = it }

    @Test
    fun `Initial toggle state cannot be removed`() {
        prepareToggle(enabled = true)
        assertTrue(toggle.enabled)
        toggle.pop()
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Enable for block`() {
        prepareToggle(enabled = false)
        assertFalse(toggle.enabled)
        toggle.enable {
            assertTrue(toggle.enabled)
        }
        assertFalse(toggle.enabled)
    }

    @Test
    fun `Disable for block`() {
        prepareToggle(enabled = true)
        assertTrue(toggle.enabled)
        toggle.disable {
            assertFalse(toggle.enabled)
        }
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Enable until previous state requested`() {
        prepareToggle(enabled = false)
        assertFalse(toggle.enabled)
        toggle.enable {
            assertTrue(toggle.enabled)
        }
        assertFalse(toggle.enabled)
    }

    @Test
    fun `keep enabled`() {
        prepareToggle(enabled = true)
        assertTrue(toggle.enabled)
        toggle.enable {
            assertTrue(toggle.enabled)
        }
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Disable until previous state requested`() {
        prepareToggle(enabled = true)
        assertTrue(toggle.enabled)
        toggle.disable {
            assertFalse(toggle.enabled)
        }
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Pop to previous state in multiple blocks`() {
        prepareToggle(enabled = false)
        assertFalse(toggle.enabled)
        toggle.enable {
            assertTrue(toggle.enabled)
            toggle.disable {
                assertFalse(toggle.enabled)
                toggle.enable {
                    assertTrue(toggle.enabled)
                }
                assertFalse(toggle.enabled)
            }
            assertTrue(toggle.enabled)
        }
        assertFalse(toggle.enabled)
    }

    @Test
    fun `Disabled is the same as not enabled`() {
        prepareToggle(enabled = false)
        assertFalse(toggle.enabled)
        assertTrue(toggle.disabled)
        toggle.enable {
            assertTrue(toggle.enabled)
            assertFalse(toggle.disabled)
        }
        assertFalse(toggle.enabled)
        assertTrue(toggle.disabled)
    }

    @Test
    fun `enable multiple toggles`() {
        val toggle1 = prepareToggle(false)
        val toggle2 = prepareToggle(false)
        val toggle3 = prepareToggle(false)

        assertFalse(toggle1.enabled)
        assertFalse(toggle2.enabled)
        assertFalse(toggle3.enabled)

        var blittKjørt = false
        (toggle1 + toggle2 + toggle3).enable {
            blittKjørt = true
            assertTrue(toggle1.enabled)
            assertTrue(toggle2.enabled)
            assertTrue(toggle3.enabled)
        }
        assertTrue(blittKjørt)

        assertFalse(toggle1.enabled)
        assertFalse(toggle2.enabled)
        assertFalse(toggle3.enabled)
    }

    @Test
    fun `disable multiple toggles`() {
        val toggle1 = prepareToggle(true)
        val toggle2 = prepareToggle(true)
        val toggle3 = prepareToggle(true)

        assertFalse(toggle1.disabled)
        assertFalse(toggle2.disabled)
        assertFalse(toggle3.disabled)

        var blittKjørt = false
        (toggle1 + toggle2 + toggle3).disable {
            blittKjørt = true
            assertTrue(toggle1.disabled)
            assertTrue(toggle2.disabled)
            assertTrue(toggle3.disabled)
        }
        assertTrue(blittKjørt)

        assertFalse(toggle1.disabled)
        assertFalse(toggle2.disabled)
        assertFalse(toggle3.disabled)
    }
}
