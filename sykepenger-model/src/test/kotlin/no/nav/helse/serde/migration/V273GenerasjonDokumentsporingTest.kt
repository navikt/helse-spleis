package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V273GenerasjonDokumentsporingTest: MigrationTest(V273GenerasjonDokumentsporing()) {

    @Test
    fun `legger til dokumentsporing`() {
        assertMigration("/migrations/273/expected.json", "/migrations/273/original.json")
    }
}