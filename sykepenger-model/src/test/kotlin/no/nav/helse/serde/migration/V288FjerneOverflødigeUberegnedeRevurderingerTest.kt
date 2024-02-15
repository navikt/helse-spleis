package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V288FjerneOverflødigeUberegnedeRevurderingerTest: MigrationTest(V288FjerneOverflødigeUberegnedeRevurderinger()) {

    @Test
    fun `fjerner overflødige uberegnede generasjoner`() {
        assertMigration("/migrations/288/expected.json", "/migrations/288/original.json")
    }
}