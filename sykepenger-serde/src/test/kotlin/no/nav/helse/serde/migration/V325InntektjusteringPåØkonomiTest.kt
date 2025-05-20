package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V325InntektjusteringPåØkonomiTest : MigrationTest(V325InntektjusteringPåØkonomi()) {

    @Test
    fun `migrerer inn inntektjustering`() {
        assertMigration("/migrations/325/expected.json", "/migrations/325/original.json")
    }
}
