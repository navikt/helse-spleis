package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V238KobleSaksbehandlerinntekterTilDenOverstyrteTest: MigrationTest(V238KobleSaksbehandlerinntekterTilDenOverstyrte()) {
    @Test
    fun `migrerer overstyrtInntektId`() {
        assertMigration(
            expectedJson = "/migrations/238/expected.json",
            originalJson = "/migrations/238/original.json"
        )
    }
}