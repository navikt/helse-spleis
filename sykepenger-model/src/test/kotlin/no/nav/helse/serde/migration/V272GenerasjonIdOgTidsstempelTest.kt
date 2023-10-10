package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V272GenerasjonIdOgTidsstempelTest: MigrationTest(V272GenerasjonIdOgTidsstempel()) {

    @Test
    fun `legger til id og tidsstempel`() {
        assertMigration("/migrations/272/expected.json", "/migrations/272/original.json")
    }
}