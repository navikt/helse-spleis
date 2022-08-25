package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V173FjerneUbrukteVilkårsgrunnlagTest : MigrationTest(V173FjerneUbrukteVilkårsgrunnlag()) {
    @Test
    fun `migrerer`() {
        assertMigration(
            expectedJson = "/migrations/173/expected.json",
            originalJson = "/migrations/173/original.json"
        )
    }
}