package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V167ProblemDagKildeTest: MigrationTest(V167ProblemDagKilde()) {

    @Test
    fun `migrerer kilde`() {
        assertMigration(
            expectedJson = "/migrations/167/expected.json",
            originalJson = "/migrations/167/original.json"
        )
    }
}