package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V296SkjæringstidspunktPåBehandlingerTest: MigrationTest(V296SkjæringstidspunktPåBehandlinger()) {

    @Test
    fun `migrerer skjæringstidspunkt på behandlinger`() {
        assertMigration("/migrations/296/expected.json", "/migrations/296/original.json")
    }
}