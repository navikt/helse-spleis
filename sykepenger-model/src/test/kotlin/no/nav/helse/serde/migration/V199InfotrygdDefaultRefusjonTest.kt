package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V199InfotrygdDefaultRefusjonTest : MigrationTest(V199InfotrygdDefaultRefusjon()) {

    @Test
    fun `lager default refusjonsopplysning for Infotryginntekter`() {
        assertMigration(
            expectedJson = "/migrations/199/expected.json",
            originalJson = "/migrations/199/original.json"
        )
    }
}