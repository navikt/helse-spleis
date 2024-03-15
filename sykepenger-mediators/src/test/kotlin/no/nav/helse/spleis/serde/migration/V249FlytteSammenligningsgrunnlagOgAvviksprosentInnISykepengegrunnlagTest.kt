package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V249FlytteSammenligningsgrunnlagOgAvviksprosentInnISykepengegrunnlagTest: MigrationTest(V249FlytteSammenligningsgrunnlagOgAvviksprosentInnISykepengegrunnlag()) {

    @Test
    fun `migrerer vilkårsgrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/249/expected.json",
            originalJson = "/migrations/249/original.json"
        )
    }
}