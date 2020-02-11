package no.nav.helse

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomManagerTest {

    private val kapasitet = 5
    private lateinit var manager: SykdomManager

    @BeforeEach
    fun setup() {
        manager = SykdomManager(makskapasitet = kapasitet)
    }

    @Test
    internal fun `Makskapasitet`() {
        repeat(kapasitet) {
            assertTrue(manager.registrer(it.toString()))
        }
        assertFalse(manager.registrer("2011"))
    }

    @Test
    internal fun `Makskapasitet med duplikat`() {
        repeat(kapasitet - 1) {
            assertTrue(manager.registrer(it.toString()))
        }
        assertTrue(manager.registrer("2011"))
        assertTrue(manager.registrer("2011"))
        assertFalse(manager.registrer("2012"))
    }

    @Test
    internal fun `Duplikat`() {
        assertTrue(manager.registrer("1"))
        assertTrue(manager.registrer("1"))
    }

    @Test
    internal fun `Registrerer`() {
        manager = SykdomManager.gjennopprett(listOf("1", "2", "3", "4"), 4)
        assertFalse(manager.registrer("5"))
    }

}

