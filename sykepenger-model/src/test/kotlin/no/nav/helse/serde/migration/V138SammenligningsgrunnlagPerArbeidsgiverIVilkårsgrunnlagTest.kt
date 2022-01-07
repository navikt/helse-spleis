package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V138SammenligningsgrunnlagPerArbeidsgiverIVilkårsgrunnlagTest : MigrationTest(V138SammenligningsgrunnlagPerArbeidsgiverIVilkårsgrunnlag()) {

    @Test
    fun `migrerer sammenligningsgrunnlag per arbeidsgiver`() {
        assertMigration(
            expectedJson = "/migrations/138/expectedFørstegangsbehandlingFlereAG.json",
            originalJson = "/migrations/138/originalFørstegangsbehandlingFlereAG.json"
        )
    }

    @Test
    fun `migrerer ikke sammenligningsgrunnlag for infotrygdforlengelser`() {
        assertMigration(
            expectedJson = "/migrations/138/expectedInfotrygdOvergang.json",
            originalJson = "/migrations/138/originalInfotrygdOvergang.json"
        )
    }
}
