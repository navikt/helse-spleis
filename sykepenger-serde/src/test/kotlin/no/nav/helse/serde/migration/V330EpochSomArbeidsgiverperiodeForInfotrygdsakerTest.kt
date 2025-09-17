package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V330EpochSomArbeidsgiverperiodeForInfotrygdsakerTest: MigrationTest(V330EpochSomArbeidsgiverperiodeForInfotrygdsaker()) {

    @Test
    fun `migrerer arbeidsgiverperiode`() {
        assertMigration("/migrations/330/expected.json", "/migrations/330/original.json")
    }
}
