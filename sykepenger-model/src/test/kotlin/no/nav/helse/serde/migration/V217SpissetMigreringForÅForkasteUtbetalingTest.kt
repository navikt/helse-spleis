package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V217SpissetMigreringForÅForkasteUtbetalingTest: MigrationTest(V217SpissetMigreringForÅForkasteUtbetaling()) {

    @Test
    fun `forkaster trøblete utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/217/expected.json",
            originalJson = "/migrations/217/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}