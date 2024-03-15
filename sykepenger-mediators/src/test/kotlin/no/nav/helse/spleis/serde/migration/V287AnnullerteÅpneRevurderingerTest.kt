package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V287AnnullerteÅpneRevurderingerEnGangTilTest: MigrationTest(V287AnnullerteÅpneRevurderingerEnGangTil()) {

    @Test
    fun `smelter sammen uberegnet revurdering med til_infotrygd`() {
        assertMigration("/migrations/287/expected.json", "/migrations/287/original.json")
    }
}