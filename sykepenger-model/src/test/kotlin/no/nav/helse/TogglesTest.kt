package no.nav.helse

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.ReflectInstance.Companion.set
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TogglesTest {

    //Trenger bare å peke på en hvilken som helst toggle
    private val toggle = Toggles.SpeilInntekterVol2Enabled

    private val initialToggleValues = toggle.let {
        it.get<MutableList<Boolean>>("states").first() to it.get<Boolean>("force")
    }

    private fun prepareToggle(enabled: Boolean, force: Boolean) {
        toggle["force"] = force
        toggle.get<MutableList<Boolean>>("states").apply {
            clear()
            add(enabled)
        }
    }

    @AfterAll
    fun tearDown() {
        prepareToggle(enabled = initialToggleValues.first, force = initialToggleValues.second)
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
