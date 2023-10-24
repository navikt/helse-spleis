package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V280HengendeRevurderingerTest: MigrationTest(V280HengendeRevurderinger()) {

    @Test
    fun `fjerner hengende uberegnede revurderinger`() {
        assertMigration("/migrations/280/expected.json", "/migrations/280/original.json")
    }
}