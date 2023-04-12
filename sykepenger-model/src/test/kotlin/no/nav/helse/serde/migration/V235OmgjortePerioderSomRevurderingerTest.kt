package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V235OmgjortePerioderSomRevurderingerTest: MigrationTest(V235OmgjortePerioderSomRevurderinger()) {
    @Test
    fun `endrer revurdering til utbetaling`() {
        assertMigration(
            expectedJson = "/migrations/235/expected.json",
            originalJson = "/migrations/235/original.json"
        )
    }
}