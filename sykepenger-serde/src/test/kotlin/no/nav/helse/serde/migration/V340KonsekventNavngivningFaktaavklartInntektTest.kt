package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V340KonsekventNavngivningFaktaavklartInntektTest: MigrationTest(V340KonsekventNavngivningFaktaavklartInntekt()) {

    @Test
    fun `Konsekven navngivning av faktaavklart inntekt`() {
        assertMigration("/migrations/340/expected.json", "/migrations/340/original.json")
    }
}
