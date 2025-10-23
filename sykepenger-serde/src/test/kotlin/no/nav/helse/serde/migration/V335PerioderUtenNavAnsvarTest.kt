package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V335PerioderUtenNavAnsvarTest: MigrationTest(V335PerioderUtenNavAnsvar()) {

    @Test
    fun `migrerer arbeidsgiverperioder`() {
        assertMigration("/migrations/335/expected.json", "/migrations/335/original.json")
    }
}
