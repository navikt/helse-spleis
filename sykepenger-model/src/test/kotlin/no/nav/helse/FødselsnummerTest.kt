package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FødselsnummerTest {

    @Test
    fun `to instanser av det samme fødselsnummeret er like`() {
        listOf("01010112345", "01030154321", "10101012345", "31122199999").forEach {
            assertEquals(it.somFødselsnummer(), it.somFødselsnummer())
            assertEquals(it.somFødselsnummer().somLong(), it.somFødselsnummer().somLong())
            assertEquals(it.somFødselsnummer().hashCode(), it.somFødselsnummer().hashCode())
        }
    }

    @Test
    fun `ulike fnr er ulike`() {
        val a = "01010112345"
        val b = "01010112346"
        assertNotEquals(a.somFødselsnummer(), b.somFødselsnummer())
        assertNotEquals(a.somFødselsnummer().somLong(), b.somFødselsnummer().somLong())
        assertNotEquals(a.somFødselsnummer().hashCode(), b.somFødselsnummer().hashCode())
    }

    @Test
    fun `ting som ikke er fnr`() {
        assertThrows<RuntimeException> { "010101123456".somFødselsnummer() }
        assertThrows<RuntimeException> { "0101011234".somFødselsnummer() }
        assertThrows<RuntimeException> { "1010112345".somFødselsnummer() }
        assertThrows<RuntimeException> { "1o101123456".somFødselsnummer() }
        assertThrows<RuntimeException> { "l0101123456".somFødselsnummer() }
        assertThrows<RuntimeException> { "1010112345s".somFødselsnummer() }
        assertThrows<RuntimeException> { "Jørgen Hattemaker".somFødselsnummer() }
        assertThrows<RuntimeException> { "".somFødselsnummer() }
        assertThrows<RuntimeException> { "1".somFødselsnummer() }
    }
}
