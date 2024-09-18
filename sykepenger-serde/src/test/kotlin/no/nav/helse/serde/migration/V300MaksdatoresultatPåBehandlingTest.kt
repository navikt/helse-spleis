package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V300MaksdatoresultatPåBehandlingTest : MigrationTest(V300MaksdatoresultatPåBehandling()) {

    @Test
    fun `migrerer maksdatoresultat`() {
        assertMigration("/migrations/300/expected.json", "/migrations/300/original.json")
    }
}