package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V301MaksdatoresultatPåBehandlingTest : MigrationTest(V301MaksdatoresultatPåBehandling()) {

    @Test
    fun `migrerer maksdatoresultat`() {
        assertMigration("/migrations/301/expected.json", "/migrations/301/original.json")
    }
}