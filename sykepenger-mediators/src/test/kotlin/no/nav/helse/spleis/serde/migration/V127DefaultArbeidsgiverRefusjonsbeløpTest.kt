package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V127DefaultArbeidsgiverRefusjonsbeløpTest : MigrationTest(V127DefaultArbeidsgiverRefusjonsbeløp()) {

    @Test
    fun `setter defaultverdi for arbeidsgiverRefusjonsbeløp`() {
        assertMigration(
            expectedJson = "/migrations/127/expected.json",
            originalJson = "/migrations/127/original.json"
        )
    }
}
