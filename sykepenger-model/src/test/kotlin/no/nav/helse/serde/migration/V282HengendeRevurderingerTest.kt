package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V282HengendeRevurderingerTest: MigrationTest(V282HengendeRevurderinger()) {

    @Test
    fun `fjerner hengende uberegnede revurderinger`() {
        assertMigration("/migrations/282/expected.json", "/migrations/282/original.json")
    }
}