package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V259NormalisereGradTest: MigrationTest(V259NormalisereGrad()) {

    @Test
    fun `endrer grad til double`() {
        assertMigration(
            expectedJson = "/migrations/259/expected.json",
            originalJson = "/migrations/259/original.json"
        )
    }
}