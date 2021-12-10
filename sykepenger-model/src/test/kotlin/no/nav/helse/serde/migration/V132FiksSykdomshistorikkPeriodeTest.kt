package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V132FiksSykdomshistorikkPeriodeTest : MigrationTest(V132FiksSykdomshistorikkPeriode()) {
    @Test
    fun `fikser periode på sykdomshistorikkelementer`() {
        assertMigration(
            expectedJson = "/migrations/132/expected.json",
            originalJson = "/migrations/132/original.json"
        )
    }
}
