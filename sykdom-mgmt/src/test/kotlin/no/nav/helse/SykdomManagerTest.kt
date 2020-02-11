package no.nav.helse

import org.junit.jupiter.api.Assertions.*
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
    internal fun `makskapasitet`() {
        repeat(kapasitet) {
            assertTrue(manager.registrer(it.toString()))
        }
        assertFalse(manager.registrer("2011"))
    }

    @Test
    internal fun `makskapasitet med duplikat`() {
        repeat(kapasitet - 1) {
            assertTrue(manager.registrer(it.toString()))
        }
        assertPersonRegistrert("2011")
        assertDuplikat("2011")
        assertFalse(manager.registrer("2012"))
    }

    @Test
    internal fun `duplikat`() {
        assertPersonRegistrert("1")
        assertDuplikat("1")
    }

    @Test
    internal fun `gjennopprett`() {
        manager = SykdomManager.gjennopprett(listOf("1", "2", "3", "4"), 4)
        assertFalse(manager.registrer("5"))
    }

    @Test
    internal fun `gjenværende kapasitet`() {
        assertPersonRegistrert("1", kapasitet-1)
    }

    private fun assertPersonRegistrert(fnr: String, gjenværendekapasitet: Int? = null) {
        var observerKalt = false
        manager.addObserver(object : SykdomManager.Observer {
            override fun personRegistrert(fødselsnummer: String, kapasitet: Int) {
                observerKalt = true
                assertEquals(fnr, fødselsnummer)
                gjenværendekapasitet?.also { assertEquals(it, kapasitet) }
            }
        })
        assertTrue(manager.registrer(fnr))
        assertTrue(observerKalt)
    }

    private fun assertDuplikat(fnr: String) {
        var observerKalt = false
        manager.addObserver(object : SykdomManager.Observer {
            override fun personRegistrert(fødselsnummer: String, kapasitet: Int) {
                observerKalt = true
            }
        })
        assertTrue(manager.registrer(fnr))
        assertFalse(observerKalt)
    }

}

