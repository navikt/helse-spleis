package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V303KopiereMaksdatoFraUtbetalingTilBehandlingTest : MigrationTest(V303KopiereMaksdatoFraUtbetalingTilBehandling()) {

    @Test
    fun `migrerer maksdato`() {
        assertMigration("/migrations/303/expected.json", "/migrations/303/original.json")
    }
}