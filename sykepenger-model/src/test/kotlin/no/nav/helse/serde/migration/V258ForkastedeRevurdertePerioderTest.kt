package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V258ForkastedeRevurdertePerioderTest: MigrationTest(V258ForkastedeRevurdertePerioder()) {

    @Test
    fun `endrer tilstander`() {
        assertMigration(
            expectedJson = "/migrations/258/expected.json",
            originalJson = "/migrations/258/original.json"
        )
    }
}