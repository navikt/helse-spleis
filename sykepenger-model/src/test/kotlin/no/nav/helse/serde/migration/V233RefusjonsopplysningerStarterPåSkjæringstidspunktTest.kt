package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V233RefusjonsopplysningerStarterPåSkjæringstidspunktTest: MigrationTest(V233RefusjonsopplysningerStarterPåSkjæringstidspunkt()) {
    @Test
    fun `setter fom til skjæringstidspunkt`() {
        assertMigration(
            expectedJson = "/migrations/233/expected.json",
            originalJson = "/migrations/233/original.json"
        )
    }
}