package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V317SykNavIEgenListeTest : MigrationTest(V317SykNavIEgenListe()) {

    @Test
    fun `migrerer sykdag`() {
        assertMigration("/migrations/317/expected.json", "/migrations/317/original.json")
    }
}
