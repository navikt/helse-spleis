package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V162ForkasteTommeVedtaksperioderTest: MigrationTest(V162ForkasteTommeVedtaksperioder()) {
    @Test
    fun `migrerer`() {
        assertMigration("/migrations/162/expected.json", "/migrations/162/original.json")
    }
}