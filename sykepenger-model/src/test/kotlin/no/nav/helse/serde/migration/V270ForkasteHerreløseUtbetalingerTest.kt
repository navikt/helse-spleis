package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V270ForkasteHerreløseUtbetalingerTest: MigrationTest(V270ForkasteHerreløseUtbetalinger()) {

    @Test
    fun `forkaster utbetalinger`() {
        assertMigration("/migrations/270/expected.json", "/migrations/270/original.json")
    }
}