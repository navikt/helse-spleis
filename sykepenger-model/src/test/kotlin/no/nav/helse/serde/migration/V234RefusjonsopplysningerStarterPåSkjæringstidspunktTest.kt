package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V234RefusjonsopplysningerStarterPåSkjæringstidspunktTest: MigrationTest(V234RefusjonsopplysningerStarterPåSkjæringstidspunkt()) {
    @Test
    fun `setter fom til skjæringstidspunkt`() {
        assertMigration(
            expectedJson = "/migrations/234/expected.json",
            originalJson = "/migrations/234/original.json"
        )
    }
}