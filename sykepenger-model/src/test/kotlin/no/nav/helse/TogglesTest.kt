package no.nav.helse

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TogglesTest {

    private lateinit var toggle: Toggles

    private fun prepareToggle(enabled: Boolean, force: Boolean) {
        toggle = object : Toggles(enabled, force) {}
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
        toggle.enable()
        assertTrue(toggle.enabled)
        toggle.pop()
        assertFalse(toggle.enabled)
    }

    @Test
    fun `Disable until previous state requested`() {
        prepareToggle(enabled = true, force = false)
        assertTrue(toggle.enabled)
        toggle.disable()
        assertFalse(toggle.enabled)
        toggle.pop()
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Keep disabled if forced state`() {
        prepareToggle(enabled = false, force = true)
        assertFalse(toggle.enabled)
        toggle.enable()
        assertFalse(toggle.enabled)
        toggle.pop()
        assertFalse(toggle.enabled)
    }

    @Test
    fun `Keep enabled if forced state`() {
        prepareToggle(enabled = true, force = true)
        assertTrue(toggle.enabled)
        toggle.disable()
        assertTrue(toggle.enabled)
        toggle.pop()
        assertTrue(toggle.enabled)
    }

    @Test
    fun `Pop to previous state in multiple blocks`() {
        prepareToggle(enabled = false, force = false)
        assertFalse(toggle.enabled)
        toggle.enable()
        assertTrue(toggle.enabled)
        toggle.disable {
            assertFalse(toggle.enabled)
            toggle.enable {
                assertTrue(toggle.enabled)
            }
            assertFalse(toggle.enabled)
        }
        assertTrue(toggle.enabled)
        toggle.pop()
        assertFalse(toggle.enabled)
    }
}
