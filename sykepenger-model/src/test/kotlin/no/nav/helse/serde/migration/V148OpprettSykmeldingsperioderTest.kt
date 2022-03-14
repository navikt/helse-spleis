package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V148OpprettSykmeldingsperioderTest : MigrationTest(V148OpprettSykmeldingsperioder()) {
    @Test
    fun `migrerer inn sykmeldingsperioder`() {
        assertMigration(
            "/migrations/148/expected.json",
            "/migrations/148/original.json"
        )
    }
}
