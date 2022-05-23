package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V165TrimmerVedtaksperiodeTest: MigrationTest(V165TrimmerVedtaksperiode()) {
    @Test
    fun `migrerer`() {
        assertMigration("/migrations/165/expected.json", "/migrations/165/original.json")
    }
}