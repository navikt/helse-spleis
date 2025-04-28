package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V323FikseFomPåUtbetalingerTest : MigrationTest(V323FikseFomPåUtbetalinger()) {

    @Test
    fun `migrerer inn riktig fom`() {
        assertMigration("/migrations/323/expected.json", "/migrations/323/original.json")
    }
}
