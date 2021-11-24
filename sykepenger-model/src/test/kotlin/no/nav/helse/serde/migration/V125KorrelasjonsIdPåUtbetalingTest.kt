package no.nav.helse.serde.migration

import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

internal class V125KorrelasjonsIdPåUtbetalingTest : MigrationTest(V125KorrelasjonsIdPåUtbetaling()) {

    @Test
    fun `setter korrelasjonsId`() {
        assertMigration(
            expectedJson = "/migrations/125/expected.json",
            originalJson = "/migrations/125/original.json"
        )
    }

    @Test
    fun `håndterer krisetilfelle dersom en uuid ikke er uuid`() {
        val migrert = migrer("/migrations/125/original-ugyldig-uuid.json".readResource())
        val utbetalinger = migrert.path("arbeidsgivere").path(0).path("utbetalinger")
        val første = utbetalinger.path(0).path("korrelasjonsId").asText()
        val tredje = utbetalinger.path(2).path("korrelasjonsId").asText()
        assertEquals(første, utbetalinger.path(1).path("korrelasjonsId").asText())
        assertNotEquals(første, tredje)
        assertDoesNotThrow { UUID.fromString(første) }
        assertDoesNotThrow { UUID.fromString(tredje) }
    }
}
