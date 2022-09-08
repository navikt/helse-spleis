package no.nav.helse.serde.migration

import java.util.UUID
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class V175IdPåAktiviteterTest: MigrationTest(V175IdPåAktiviteter()) {

    @Test
    fun `aktiviteter inneholder id etter migrering`() {
        val migrert = migrer("/migrations/175/original.json".readResource())
        val versjon = migrert.path("skjemaVersjon").asInt()
        val aktiviteter = migrert.path("aktivitetslogg").path("aktiviteter")
        assertEquals(175, versjon)
        aktiviteter.forEach {
            assertNotNull(it.path("id"))
            assertDoesNotThrow { UUID.fromString(it.path("id").asText()) }
        }
    }
}