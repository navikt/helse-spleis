package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V135UtbetalingslinjeGradTest : MigrationTest(V135UtbetalingslinjeGrad()) {

    @Test
    fun `runder grad ned`() {
        assertMigration(
            expectedJson = "/migrations/135/expected.json",
            originalJson = "/migrations/135/original.json"
        )
    }
}
