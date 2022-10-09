package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test


internal class V181OverstyrteVilkårsgrunnlagTest : MigrationTest(V181OverstyrteVilkårsgrunnlag()) {
    @Test
    fun `spore tilbake deaktiverte inntekter`() {
        assertMigration(
            expectedJson = "/migrations/181/expected.json",
            originalJson = "/migrations/181/original.json"
        )
    }

    @Test
    fun spesialtilfelle() {
        assertMigration(
            expectedJson = "/migrations/181/expected-spesialtilfelle.json",
            originalJson = "/migrations/181/original-spesialtilfelle.json"
        )
    }
}