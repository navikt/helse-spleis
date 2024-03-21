package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V292AnnullertPeriodeTest: MigrationTest(V292AnnullertPeriode()) {

    @Test
    fun `migrerer annullert periode`() {
        assertMigration("/migrations/292/expected.json", "/migrations/292/original.json")
    }
}