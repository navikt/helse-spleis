package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V136MigrereTilstanderPåForkastedeTest : MigrationTest(V136MigrereTilstanderPåForkastede()) {

    @Test
    fun `migrerer gamle perioder`() {
        assertMigration(
            expectedJson = "/migrations/136/expected.json",
            originalJson = "/migrations/136/original.json"
        )
    }
}
