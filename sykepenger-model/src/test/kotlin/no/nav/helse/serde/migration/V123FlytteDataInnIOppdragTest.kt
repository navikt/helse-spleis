package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V123FlytteDataInnIOppdragTest : MigrationTest(V123FlytteDataInnIOppdrag()) {

    @Test
    fun `legger til avstemmingsnøkkel, overføringstidspunkt og status på alle eksisterende oppdrag`() {
        assertMigration(
            expectedJson = "/migrations/123/personMedUtbetalingerExpected.json",
            originalJson = "/migrations/123/personMedUtbetalingerOriginal.json"
        )
    }
}
