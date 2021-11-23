package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V124SetteOppdragSomUendretTest : MigrationTest(V124SetteOppdragSomUendret()) {

    @Test
    fun `setter oppdrag uten endring til uend`() {
        assertMigration(
            expectedJson = "/migrations/124/expected.json",
            originalJson = "/migrations/124/original.json"
        )
    }
}
