package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V236MigrereUtbetalingTilÅOverlappeMedAUUTest: MigrationTest(V236MigrereUtbetalingTilÅOverlappeMedAUU()) {
    @Test
    fun `strekker tilbake utbetalingsperiodeobjekt`() {
        assertMigration(
            expectedJson = "/migrations/236/expected.json",
            originalJson = "/migrations/236/original.json"
        )
    }
}