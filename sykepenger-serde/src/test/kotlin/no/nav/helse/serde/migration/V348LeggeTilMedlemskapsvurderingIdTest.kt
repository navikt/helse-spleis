package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V348LeggeTilMedlemskapsvurderingIdTest : MigrationTest(V348LeggeTilMedlemskapsvurderingId()) {
    @Test
    fun `Legger på medlemskapsvurderingId`() {
        assertMigration("/migrations/348/expected.json", "/migrations/348/original.json")
    }
}
