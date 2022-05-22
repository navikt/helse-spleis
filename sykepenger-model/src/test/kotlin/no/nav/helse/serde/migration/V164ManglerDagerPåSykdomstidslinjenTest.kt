package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V164ManglerDagerPåSykdomstidslinjenTest: MigrationTest(V164ManglerDagerPåSykdomstidslinjen()) {
    @Test
    fun `migrerer`() {
        assertMigration("/migrations/164/expected.json", "/migrations/164/original.json")
    }
}