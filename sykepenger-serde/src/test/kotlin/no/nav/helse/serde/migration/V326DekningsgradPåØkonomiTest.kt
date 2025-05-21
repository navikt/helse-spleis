package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V326DekningsgradPåØkonomiTest : MigrationTest(V326DekningsgradPåØkonomi()) {

    @Test
    fun `migrerer inn dekningsgrad`() {
        assertMigration("/migrations/326/expected.json", "/migrations/326/original.json")
    }
}
