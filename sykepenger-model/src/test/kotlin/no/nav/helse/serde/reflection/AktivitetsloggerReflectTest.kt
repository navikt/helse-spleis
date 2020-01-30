package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class AktivitetsloggerReflectTest {
    @Test
    internal fun `kontroller at alle felter er gjort rede for`() {
        assertMembers<Aktivitetslogger, AktivitetsloggerReflect>(
            skalMappes = listOf("aktiviteter", "originalMessage")
        )
        assertMembers<Aktivitetslogger, AktivitetsloggerReflect>(
            subClasses = "Aktivitet" to "AktivitetReflect",
            skalMappes = listOf("alvorlighetsgrad", "melding", "tidsstempel")
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    internal fun `mapper Aktivitetslogger til map`() {
        val aktivitetslogger = Aktivitetslogger().apply {
            info("Test")
        }
        val map = AktivitetsloggerReflect(aktivitetslogger).toMap()

        assertEquals(2, map.size)
        assertEquals("Test", (map["aktiviteter"] as List<Map<String, Any>>)[0]["melding"])
        assertNull(map["originalMessage"])
    }
}
