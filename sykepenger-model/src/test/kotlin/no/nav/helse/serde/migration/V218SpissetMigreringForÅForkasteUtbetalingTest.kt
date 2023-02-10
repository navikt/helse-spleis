package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V218SpissetMigreringForÅForkasteUtbetalingTest: MigrationTest(V218SpissetMigreringForÅForkasteUtbetaling()) {

    @Test
    fun `forkaster trøblete utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/218/expected.json",
            originalJson = "/migrations/218/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}