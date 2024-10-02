package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V306RefusjonstidslinjePåBehandlingTest : MigrationTest(V306RefusjonstidslinjePåBehandling()) {

    @Test
    fun `migrerer refusjonstidslinje`() {
        assertMigration("/migrations/306/expected.json", "/migrations/306/original.json")
    }
}