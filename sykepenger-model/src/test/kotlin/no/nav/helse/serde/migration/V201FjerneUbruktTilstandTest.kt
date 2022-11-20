package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V201FjerneUbruktTilstandTest : MigrationTest(V201FjerneUbruktTilstand()) {
    @Test
    fun `migrer gammel tilstand`() {
        assertMigration(
            expectedJson = "/migrations/201/expected.json",
            originalJson = "/migrations/201/original.json"
        )
    }
}