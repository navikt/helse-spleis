package no.nav.helse.økonomi

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AvviksprosentTest {

    @Test
    fun `to desimaler`() {
        val avvik = Avviksprosent.avvik(25000.0, 30000.0)
        assertEquals(16.67, avvik.rundTilToDesimaler())
    }
}