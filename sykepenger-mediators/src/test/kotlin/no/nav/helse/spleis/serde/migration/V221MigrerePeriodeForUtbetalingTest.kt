package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V221MigrerePeriodeForUtbetalingTest: MigrationTest(V221MigrerePeriodeForUtbetaling()) {

    @Test
    fun `setter periode på utbetaling`() {
        assertMigration(
            expectedJson = "/migrations/221/expected.json",
            originalJson = "/migrations/221/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}