package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V261ForkastegamleUtbetalingerTest: MigrationTest(V261ForkastegamleUtbetalinger()) {

    @Test
    fun `forkaster utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/261/expected.json",
            originalJson = "/migrations/261/original.json"
        )
    }
}