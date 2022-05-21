package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V162ManglerDagerPåSykdomstidslinjenTest: MigrationTest(V162ManglerDagerPåSykdomstidslinjen()) {
    @Test
    fun `migrerer`() {
        assertMigration("/migrations/162/expected.json", "/migrations/162/original.json", JSONCompareMode.LENIENT)
    }
}