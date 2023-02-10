package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V220MigrerePeriodeForUtbetalingTest: MigrationTest(V220MigrerePeriodeForUtbetaling()) {

    @Test
    fun `setter periode på utbetaling`() {
        assertMigration(
            expectedJson = "/migrations/220/expected.json",
            originalJson = "/migrations/220/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}