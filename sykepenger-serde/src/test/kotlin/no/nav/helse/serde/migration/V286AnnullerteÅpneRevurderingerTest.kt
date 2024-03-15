package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V286AnnullerteÅpneRevurderingerTest: MigrationTest(V286AnnullerteÅpneRevurderinger()) {

    @Test
    fun `smelter sammen uberegnet revurdering med til_infotrygd`() {
        assertMigration("/migrations/286/expected.json", "/migrations/286/original.json")
    }
}