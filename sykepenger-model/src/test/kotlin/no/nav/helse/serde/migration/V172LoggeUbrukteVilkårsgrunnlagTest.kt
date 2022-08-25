package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V172LoggeUbrukteVilkårsgrunnlagTest : MigrationTest(V172LoggeUbrukteVilkårsgrunnlag()) {
    @Test
    fun `migrerer`() {
        assertMigration(
            expectedJson = "/migrations/172/expected.json",
            originalJson = "/migrations/172/original.json"
        )
    }
}