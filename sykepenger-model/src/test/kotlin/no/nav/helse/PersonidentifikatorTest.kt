package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class PersonidentifikatorTest {

    @Test
    fun `to instanser av det samme fødselsnummeret er like`() {
        listOf("01010112345", "01030154321", "10101012345", "31122199999").forEach {
            assertEquals(it.somPersonidentifikator(), it.somPersonidentifikator())
            assertEquals(it.somPersonidentifikator().toLong(), it.somPersonidentifikator().toLong())
            assertEquals(it.somPersonidentifikator().hashCode(), it.somPersonidentifikator().hashCode())
        }
    }

    @Test
    fun `ulike fnr er ulike`() {
        val a = "01010112345"
        val b = "01010112346"
        assertNotEquals(a.somPersonidentifikator(), b.somPersonidentifikator())
        assertNotEquals(a.somPersonidentifikator().toLong(), b.somPersonidentifikator().toLong())
        assertNotEquals(a.somPersonidentifikator().hashCode(), b.somPersonidentifikator().hashCode())
    }

    @Test
    fun `identitetsnummer hvor seks første siffer ikke er fødselsdato er også gyldige`() {
        assertDoesNotThrow { "32120012345".somPersonidentifikator() }
        assertDoesNotThrow { "31130012345".somPersonidentifikator() }
        assertDoesNotThrow { "32130012345".somPersonidentifikator() }
    }
}
