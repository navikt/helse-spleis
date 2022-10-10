package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test


internal class V182OverstyrteVilkårsgrunnlagTest : MigrationTest(V182OverstyrteVilkårsgrunnlag()) {
    @Test
    fun `spore tilbake deaktiverte inntekter`() {
        assertMigration(
            expectedJson = "/migrations/182/expected.json",
            originalJson = "/migrations/182/original.json"
        )
    }

    @Test
    fun spesialtilfelle() {
        assertMigration(
            expectedJson = "/migrations/182/expected-spesialtilfelle.json",
            originalJson = "/migrations/182/original-spesialtilfelle.json"
        )
    }
}