package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FødselsnummerTest {

    @Test
    fun `to instanser av det samme fødselsnummeret er like`() {
        listOf("01010112345", "01030154321", "10101012345", "31122199999").forEach {
            assertEquals(it.somFødselsnummer(), it.somFødselsnummer())
            assertEquals(it.somFødselsnummer().somLong(), it.somFødselsnummer().somLong())
            assertEquals(it.somFødselsnummer().hashCode(), it.somFødselsnummer().hashCode())
        }
    }
}
