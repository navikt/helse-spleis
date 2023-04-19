package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V241DokumenttypeSomListeTest : MigrationTest(V241DokumenttypeSomListe()) {
    @Test
    fun `migrerer hendelser`() {
        assertMigration(
            expectedJson = "/migrations/241/expected.json",
            originalJson = "/migrations/241/original.json"
        )
    }
}