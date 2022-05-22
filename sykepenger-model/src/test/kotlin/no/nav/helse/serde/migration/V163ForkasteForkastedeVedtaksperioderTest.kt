package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V163ForkasteForkastedeVedtaksperioderTest: MigrationTest(V163ForkasteForkastedeVedtaksperioder()) {
    @Test
    fun `migrerer`() {
        assertMigration("/migrations/163/expected.json", "/migrations/163/original.json")
    }
}