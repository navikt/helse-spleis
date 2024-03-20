package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V291FikseTidligereOmgjøringerSomErRevurderingFeiletTest: MigrationTest(V291FikseTidligereOmgjøringerSomErRevurderingFeilet()) {

    @Test
    fun `fjerner feilaktige behandlinger`() {
        assertMigration("/migrations/291/expected.json", "/migrations/291/original.json")
    }
}