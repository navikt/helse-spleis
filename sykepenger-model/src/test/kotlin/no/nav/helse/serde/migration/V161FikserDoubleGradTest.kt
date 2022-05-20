package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V161FikserDoubleGradTest : MigrationTest(V161FikserDoubleGrad()) {
    @Test
    fun `runder av grader`() {
        assertMigration("/migrations/161/expected.json", "/migrations/161/original.json")
    }
}