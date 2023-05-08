package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V246FinneOverlappendePerioderTest: MigrationTest(V246FinneOverlappendePerioder()) {
    @Test
    fun `migrerer perioder`() {
        assertMigration(
            expectedJson = "/migrations/246/expected.json",
            originalJson = "/migrations/246/original.json"
        )
    }
}