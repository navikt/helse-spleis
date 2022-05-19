package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V157ManglerDagerPåSykdomstidslinjenDryRunTest: MigrationTest(V157ManglerDagerPåSykdomstidslinjenDryRun()) {
    @Test
    fun `sikrer at dummy migrering ikke endrer noe`() {
        assertMigration("/migrations/157/original.json", "/migrations/157/original.json", JSONCompareMode.LENIENT)
    }
}