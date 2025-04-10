package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V321EgenmeldingsdagerPåBehandlingTest  : MigrationTest(V321EgenmeldingsdagerPåBehandling()) {

    @Test
    fun `migrerer inn egenmeldingsdager`() {
        assertMigration("/migrations/321/expected.json", "/migrations/321/original.json")
    }
}
