package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V260ForkasteUtbetalingerTest: MigrationTest(V260ForkasteUtbetalinger()) {

    @Test
    fun `forkaster utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/260/expected.json",
            originalJson = "/migrations/260/original.json"
        )
    }
}