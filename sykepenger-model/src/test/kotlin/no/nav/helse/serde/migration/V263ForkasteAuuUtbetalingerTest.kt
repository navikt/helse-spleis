package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V263ForkasteAuuUtbetalingerTest: MigrationTest(V263ForkasteAuuUtbetalinger()) {

    @Test
    fun `forkaster utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/263/expected.json",
            originalJson = "/migrations/263/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}