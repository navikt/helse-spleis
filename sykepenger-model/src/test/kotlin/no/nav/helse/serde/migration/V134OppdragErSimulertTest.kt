package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V134OppdragErSimulertTest :  MigrationTest(V134OppdragErSimulert()) {

    @Test
    fun `migrer erSimulert`() {
        assertMigration(
            expectedJson = "/migrations/134/expected.json",
            originalJson = "/migrations/134/original.json"
        )
    }
}
