package no.nav.helse.hendelser

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
