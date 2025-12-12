package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V343JordbrukerTilSelvstendigYrkesaktivitetstypeTest : MigrationTest(V343JordbrukerTilSelvstendigYrkesaktivitetstype()) {

    @Test
    fun `Endrer organisasjonsnummer og yrkesaktivitetstype fra JORDBRUKER til SELVSTENDIG`() {
        assertMigration("/migrations/343/expected.json", "/migrations/343/original.json")
    }
}
