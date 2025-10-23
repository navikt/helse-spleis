package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V336PerioderUtenNavAnsvarPåBehandlingTest: MigrationTest(V336PerioderUtenNavAnsvarPåBehandling()) {

    @Test
    fun `migrerer arbeidsgiverperiode`() {
        assertMigration("/migrations/336/expected.json", "/migrations/336/original.json")
    }
}
