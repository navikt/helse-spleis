package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V159FikserDoubleGradTest : MigrationTest(V159FikserDoubleGrad()) {
    @Test
    fun `runder av grader`() {
        assertMigration("/migrations/159/original.json", "/migrations/159/original.json", JSONCompareMode.LENIENT)
    }
}