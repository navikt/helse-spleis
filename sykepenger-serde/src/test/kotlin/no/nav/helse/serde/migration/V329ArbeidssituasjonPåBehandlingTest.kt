package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V329ArbeidssituasjonPåBehandlingTest: MigrationTest(V329ArbeidssituasjonPåBehandling()) {

    @Test
    fun `migrerer arbeidssituasjon`() {
        assertMigration("/migrations/329/expected.json", "/migrations/329/original.json")
    }
}
