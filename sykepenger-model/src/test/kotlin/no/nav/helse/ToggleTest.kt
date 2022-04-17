package no.nav.helse

import no.nav.helse.Toggle.Companion.disable
import no.nav.helse.Toggle.Companion.enable
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ToggleTest {

    private lateinit var toggle: Toggle

    private fun prepareToggle(enabled: Boolean, force: Boolean) {
        toggle = object : Toggle(enabled, force) {}
    }

    @Test
    fun `Initial toggle state cannot be removed`() {
        prepareToggle(enabled = true, force = false)
        assertTrue(toggle.enabled)
        toggle.pop()
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Enable for block`() {
        prepareToggle(enabled = false, force = false)
        assertFalse(toggle.enabled)
        toggle.enable {
            assertTrue(toggle.enabled)
        }
        assertFalse(toggle.enabled)
    }

    @Test
    fun `Disable for block`() {
        prepareToggle(enabled = true, force = false)
        assertTrue(toggle.enabled)
        toggle.disable {
            assertFalse(toggle.enabled)
        }
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Keep disabled for block if forced`() {
        prepareToggle(enabled = false, force = true)
        assertFalse(toggle.enabled)
        toggle.enable {
            assertFalse(toggle.enabled)
        }
        assertFalse(toggle.enabled)
    }

    @Test
    fun `Keep enabled for block if forced`() {
        prepareToggle(enabled = true, force = true)
        assertTrue(toggle.enabled)
        toggle.disable {
            assertTrue(toggle.enabled)
        }
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Enable until previous state requested`() {
        prepareToggle(enabled = false, force = false)
        assertFalse(toggle.enabled)
        toggle.enable {
            assertTrue(toggle.enabled)
        }
        assertFalse(toggle.enabled)
    }

    @Test
    fun `keep enabled`() {
        prepareToggle(enabled = true, force = false)
        assertTrue(toggle.enabled)
        toggle.enable {
            assertTrue(toggle.enabled)
        }
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Disable until previous state requested`() {
        prepareToggle(enabled = true, force = false)
        assertTrue(toggle.enabled)
        toggle.disable {
            assertFalse(toggle.enabled)
        }
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Keep disabled if forced state`() {
        prepareToggle(enabled = false, force = true)
        assertFalse(toggle.enabled)
        toggle.enable {
            assertFalse(toggle.enabled)
        }
        assertFalse(toggle.enabled)
    }

    @Test
    fun `Keep enabled if forced state`() {
        prepareToggle(enabled = true, force = true)
        assertTrue(toggle.enabled)
        toggle.disable {
            assertTrue(toggle.enabled)
        }
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Pop to previous state in multiple blocks`() {
        prepareToggle(enabled = false, force = false)
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
        prepareToggle(enabled = false, force = false)
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
        val toggle1 = object : Toggle(enabled = false, force = false) {}
        val toggle2 = object : Toggle(enabled = false, force = false) {}
        val toggle3 = object : Toggle(enabled = false, force = false) {}

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
        val toggle1 = object : Toggle(enabled = true, force = false) {}
        val toggle2 = object : Toggle(enabled = true, force = false) {}
        val toggle3 = object : Toggle(enabled = true, force = false) {}

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

    @Test
    fun `enable multiple toggles - keep disabled when forced`() {
        val toggle1 = object : Toggle(enabled = false, force = false) {}
        val toggle2 = object : Toggle(enabled = false, force = true) {}
        val toggle3 = object : Toggle(enabled = false, force = false) {}

        assertFalse(toggle1.enabled)
        assertFalse(toggle2.enabled)
        assertFalse(toggle3.enabled)

        var blittKjørt = false
        (toggle1 + toggle2 + toggle3).enable {
            blittKjørt = true
            assertTrue(toggle1.enabled)
            assertFalse(toggle2.enabled)
            assertTrue(toggle3.enabled)
        }
        assertTrue(blittKjørt)

        assertFalse(toggle1.enabled)
        assertFalse(toggle2.enabled)
        assertFalse(toggle3.enabled)
    }

    @Test
    fun `disable multiple toggles - keep enabled when forced`() {
        val toggle1 = object : Toggle(enabled = true, force = true) {}
        val toggle2 = object : Toggle(enabled = true, force = false) {}
        val toggle3 = object : Toggle(enabled = true, force = true) {}

        assertFalse(toggle1.disabled)
        assertFalse(toggle2.disabled)
        assertFalse(toggle3.disabled)

        var blittKjørt = false
        (toggle1 + toggle2 + toggle3).disable {
            blittKjørt = true
            assertFalse(toggle1.disabled)
            assertTrue(toggle2.disabled)
            assertFalse(toggle3.disabled)
        }
        assertTrue(blittKjørt)

        assertFalse(toggle1.disabled)
        assertFalse(toggle2.disabled)
        assertFalse(toggle3.disabled)
    }
}
