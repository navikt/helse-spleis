package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V247ForkasteOverlappendeTest: MigrationTest(V247ForkasteOverlappende()) {
    @Test
    fun `migrerer perioder`() {
        assertMigration(
            expectedJson = "/migrations/247/expected.json",
            originalJson = "/migrations/247/original.json"
        )
    }
}