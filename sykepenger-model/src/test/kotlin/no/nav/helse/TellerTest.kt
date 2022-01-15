package no.nav.helse

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TellerTest {

    private val grenseverdi = 10
    private lateinit var observatør: Observatør
    private lateinit var teller: Teller

    @Test
    fun `øke nullteller`() {
        teller = Teller(0)
        teller.observer(observatør)
        teller.inc()
        assertFalse(observatør.grenseNådd)
        assertEquals(0, observatør.grenserNådd)
        assertFalse(observatør.reset)
    }

    @Test
    fun `øke nullteller flere ganger`() {
        teller = Teller(0)
        teller.observer(observatør)
        teller.inc()
        teller.inc()
        assertFalse(observatør.grenseNådd)
        assertEquals(0, observatør.grenserNådd)
        assertFalse(observatør.reset)
    }

    @Test
    fun `resette nullteller`() {
        teller = Teller(0)
        teller.observer(observatør)
        teller.inc()
        teller.reset()
        teller.inc()
        assertFalse(observatør.grenseNådd)
        assertEquals(0, observatør.grenserNådd)
        assertFalse(observatør.reset)
    }

    @Test
    fun `før grense`() {
        repeat(grenseverdi - 1) { teller.inc() }
        assertEquals(grenseverdi - 1, observatør.increments)
        assertFalse(observatør.grenseNådd)
        assertFalse(observatør.reset)
    }

    @Test
    fun `på grense`() {
        repeat(grenseverdi) { teller.inc() }
        assertEquals(grenseverdi, observatør.increments)
        assertTrue(observatør.grenseNådd)
        assertFalse(observatør.reset)
    }

    @Test
    fun `etter grense`() {
        repeat(grenseverdi + 1) { teller.inc() }
        assertEquals(grenseverdi + 1, observatør.increments)
        assertTrue(observatør.grenseNådd)
        assertEquals(1, observatør.grenserNådd)
        assertFalse(observatør.reset)
    }

    @Test
    fun `tilbakestille før grense`() {
        repeat(grenseverdi - 1) { teller.inc() }
        teller.reset()
        teller.inc()
        assertEquals(grenseverdi, observatør.increments)
        assertFalse(observatør.grenseNådd)
        assertTrue(observatør.reset)
    }

    @Test
    fun `tilbakestille etter grense`() {
        repeat(grenseverdi) { teller.inc() }
        teller.reset()
        repeat(grenseverdi) { teller.inc() }
        assertEquals(2*grenseverdi, observatør.increments)
        assertEquals(2, observatør.grenserNådd)
        assertTrue(observatør.grenseNådd)
        assertTrue(observatør.reset)
    }

    @BeforeEach
    fun setup() {
        observatør = Observatør()
        teller = Teller(grenseverdi)
        teller.observer(observatør)
    }

    private class Observatør : Teller.Observer {
        var increments = 0
        var grenserNådd = 0
        var grenseNådd = false
        var reset = false

        override fun onInc() { increments +=1 }
        override fun onGrense() {
            grenseNådd = true
            grenserNådd += 1
        }
        override fun onReset() { reset = true }
    }
}
