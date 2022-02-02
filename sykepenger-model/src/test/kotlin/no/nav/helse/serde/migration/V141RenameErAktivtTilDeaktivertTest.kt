package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V141RenameErAktivtTilDeaktivertTest : MigrationTest(V141RenameErAktivtTilDeaktivert()) {
    @Test
    fun `endrer navn for erAktivt til deaktivert og flipper verdien`() {
        assertMigration(
            "/migrations/141/expected.json",
            "/migrations/141/original.json"
        )
    }
}
