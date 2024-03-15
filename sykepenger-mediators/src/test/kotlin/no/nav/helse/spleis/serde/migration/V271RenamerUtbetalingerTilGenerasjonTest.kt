package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V271RenamerUtbetalingerTilGenerasjonTest: MigrationTest(V271RenamerUtbetalingerTilGenerasjon()) {

    @Test
    fun `renamer utbetalinger`() {
        assertMigration("/migrations/271/expected.json", "/migrations/271/original.json")
    }
}