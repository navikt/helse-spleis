package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V156ManglerDagerPåSykdomstidslinjenDryRunTest: MigrationTest(V156ManglerDagerPåSykdomstidslinjenDryRun()) {
    @Test
    fun `sikrer at dummy migrering ikke endrer noe`() {
        assertMigration("/migrations/156/original.json", "/migrations/156/original.json", JSONCompareMode.LENIENT)
    }
}