package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V302MaksdatoresultatPåBehandlingTest : MigrationTest(V302MaksdatoresultatPåBehandling()) {

    @Test
    fun `migrerer maksdatoresultat`() {
        assertMigration("/migrations/302/expected.json", "/migrations/302/original.json")
    }
}