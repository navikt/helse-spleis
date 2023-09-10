package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V264ForkasteAuuUtbetalingerTest: MigrationTest(V264ForkasteAuuUtbetalinger()) {

    @Test
    fun `forkaster utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/264/expected.json",
            originalJson = "/migrations/264/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}