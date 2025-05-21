package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V327ProsenterSomTallMellomNullOgEnTest : MigrationTest(V327ProsenterSomTallMellomNullOgEn()) {

    @Test
    fun `migrerer prosenter`() {
        assertMigration("/migrations/327/expected.json", "/migrations/327/original.json")
    }
}
