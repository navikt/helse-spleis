package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V184SetteSkjæringstidspunktEldreVilkårsgrunnlagTest : MigrationTest(V184SetteSkjæringstidspunktEldreVilkårsgrunnlag()) {
    @Test
    fun `setter skjæringstidspunkt på eldre innslag`() {
        assertMigration(
            expectedJson = "/migrations/184/expected.json",
            originalJson = "/migrations/184/original.json"
        )
    }
}