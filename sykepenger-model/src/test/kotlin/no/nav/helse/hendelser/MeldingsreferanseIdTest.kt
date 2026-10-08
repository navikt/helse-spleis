package no.nav.helse.hendelser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class MeldingsreferanseIdTest {
    @Test
    fun meldingsreferanseId() {
        val id = UUID.randomUUID()
        val a = MeldingsreferanseId(id)
        val b = MeldingsreferanseId(id)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
