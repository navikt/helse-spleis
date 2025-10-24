package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V338JustereFomPåUtbetalingerTest: MigrationTest(V338JustereFomPåUtbetalinger()) {

    @Test
    fun `migrerer utbetaling-fom`() {
        assertMigration("/migrations/338/expected.json", "/migrations/338/original.json")
    }
}
