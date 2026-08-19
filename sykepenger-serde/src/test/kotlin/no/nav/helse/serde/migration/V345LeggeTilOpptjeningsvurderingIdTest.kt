package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V345LeggeTilOpptjeningsvurderingIdTest : MigrationTest(V345LeggeTilOpptjeningsvurderingId()) {

    @Test
    fun `Legger på opptjeningsvurderingId`() {
        assertMigration("/migrations/345/expected.json", "/migrations/345/original.json")
    }

}
