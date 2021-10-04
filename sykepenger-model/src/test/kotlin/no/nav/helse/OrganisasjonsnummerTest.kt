package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrganisasjonsnummerTest {

    @Test
    fun `to instanser av det samme organisasjonsnummer er like`() {
        listOf("987654321", "987654322", "888888888", "123456789").forEach {
            assertEquals(it.somOrganisasjonsnummer(), it.somOrganisasjonsnummer())
            assertEquals(it.somOrganisasjonsnummer().somLong(), it.somOrganisasjonsnummer().somLong())
            assertEquals(it.somOrganisasjonsnummer().hashCode(), it.somOrganisasjonsnummer().hashCode())
        }
    }

    @Test
    fun `ulike orgnr er ulike`() {
        val a = "987654321"
        val b = "987654322"
        assertNotEquals(a.somOrganisasjonsnummer(), b.somOrganisasjonsnummer())
        assertNotEquals(a.somOrganisasjonsnummer().somLong(), b.somOrganisasjonsnummer().somLong())
        assertNotEquals(a.somOrganisasjonsnummer().hashCode(), b.somOrganisasjonsnummer().hashCode())
    }

    @Test
    fun `ting som ikke er orgnr`() {
        assertThrows<RuntimeException> { "9876543210".somOrganisasjonsnummer() }
        assertThrows<RuntimeException> { "98765432".somOrganisasjonsnummer() }
        assertThrows<RuntimeException> { "87654321".somOrganisasjonsnummer() }
        assertThrows<RuntimeException> { "98765432l".somOrganisasjonsnummer() }
        assertThrows<RuntimeException> { "n87654321".somOrganisasjonsnummer() }
        assertThrows<RuntimeException> { "98765f321".somOrganisasjonsnummer() }
        assertThrows<RuntimeException> { "Jørgen Hattemaker".somOrganisasjonsnummer() }
        assertThrows<RuntimeException> { "".somOrganisasjonsnummer() }
        assertThrows<RuntimeException> { "1".somOrganisasjonsnummer() }
    }
}
