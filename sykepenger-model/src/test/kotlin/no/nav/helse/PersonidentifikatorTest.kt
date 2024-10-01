package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class PersonidentifikatorTest {

    @Test
    fun `to instanser av det samme fødselsnummeret er like`() {
        listOf("01010112345", "01030154321", "10101012345", "31122199999").forEach {
            assertEquals(Personidentifikator(it), Personidentifikator(it))
            assertEquals(Personidentifikator(it).toLong(), Personidentifikator(it).toLong())
            assertEquals(Personidentifikator(it).hashCode(), Personidentifikator(it).hashCode())
        }
    }

    @Test
    fun `ulike fnr er ulike`() {
        val a = "01010112345"
        val b = "01010112346"
        assertNotEquals(Personidentifikator(a), Personidentifikator(b))
        assertNotEquals(Personidentifikator(a).toLong(), Personidentifikator(b).toLong())
        assertNotEquals(Personidentifikator(a).hashCode(), Personidentifikator(b).hashCode())
    }

    @Test
    fun `identitetsnummer hvor seks første siffer ikke er fødselsdato er også gyldige`() {
        assertDoesNotThrow { Personidentifikator("32120012345") }
        assertDoesNotThrow { Personidentifikator("31130012345") }
        assertDoesNotThrow { Personidentifikator("32130012345") }
    }
}
