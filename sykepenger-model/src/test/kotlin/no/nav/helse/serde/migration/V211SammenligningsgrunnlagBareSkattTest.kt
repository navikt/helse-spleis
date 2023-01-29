package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V211SammenligningsgrunnlagBareSkattTest: MigrationTest(V211SammenligningsgrunnlagBareSkatt()) {

    @Test
    fun `migrerer sammenligningsgrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/211/expected.json",
            originalJson = "/migrations/211/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }

    @Test
    fun `migrerer ikke sammenligningsgrunnlag på infotrygd-grunnlag`() {
        assertMigration(
            expectedJson = "/migrations/211/expected-it.json",
            originalJson = "/migrations/211/original-it.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }

}