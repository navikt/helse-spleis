package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V328InntektjusteringerPåBehandlingTest: MigrationTest(V328InntektjusteringerPåBehandling()) {

    @Test
    fun `migrerer inntektjusteringer`() {
        assertMigration("/migrations/328/expected.json", "/migrations/328/original.json")
    }
}
