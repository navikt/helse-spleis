package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V232AvsluttetUtenUtbetalingLåstePerioderTest: MigrationTest(V232AvsluttetUtenUtbetalingLåstePerioder()) {

    @Test
    fun `låser ned perioder i avsluttet uten utbetaling`() {
        assertMigration(
            expectedJson = "/migrations/232/expected.json",
            originalJson = "/migrations/232/original.json"
        )
    }
}