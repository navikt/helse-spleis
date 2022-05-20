package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V160FikserDoubleGradTest : MigrationTest(V160FikserDoubleGrad()) {
    @Test
    fun `runder av grader`() {
        assertMigration("/migrations/160/expected.json", "/migrations/160/original.json")
    }
}