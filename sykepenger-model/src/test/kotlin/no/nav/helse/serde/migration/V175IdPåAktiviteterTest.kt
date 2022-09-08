package no.nav.helse.serde.migration

import java.util.UUID
import no.nav.helse.readResource
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class V175IdPåAktiviteterTest: MigrationTest(V175IdPåAktiviteter()) {

    @Test
    fun `aktiviteter inneholder id etter migrering`() {
        val migrert = migrer("/migrations/175/original.json".readResource())
        val versjon = migrert.path("skjemaVersjon").asInt()
        val aktiviteter = migrert.path("aktivitetslogg").path("aktiviteter")
        assertEquals(175, versjon)
        assertEquals(5, aktiviteter.size())
        aktiviteter.forEach {
            assertNotNull(it.path("kontekster"))
            assertNotNull(it.path("melding"))
            assertNotNull(it.path("tidsstempel"))
            assertNotNull(it.path("detaljer"))
            assertNotNull(it.path("alvorlighetsgrad"))
            assertNotNull(it.path("id"))
            assertTrue(it.path("id").isTextual)
            assertDoesNotThrow { UUID.fromString(it.path("id").asText()) }
        }
    }

    @Test
    fun `tom aktivitetslogg`() {
        assertDoesNotThrow {
            migrer(tomAktivitetslogg)
        }
    }

    @Test
    fun `aktivitetslogg uten aktiviteter`() {
        assertDoesNotThrow {
            migrer(aktivitetsloggUtenAktiviteter)
        }
    }

    @Language("JSON")
    private val tomAktivitetslogg = """{
  "skjemaVersjon": 174,
  "aktivitetslogg": {}
}
    """
    @Language("JSON")
    private val aktivitetsloggUtenAktiviteter = """{
  "skjemaVersjon": 174,
  "aktivitetslogg": {
    "aktiviteter": []
  }
}
    """
}