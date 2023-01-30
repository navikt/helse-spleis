package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V214FjernerInfotrygdOgSkattSykepengegrunnlagFraInntektshistorikkenTest: MigrationTest(V214FjernerInfotrygdOgSkattSykepengegrunnlagFraInntektshistorikken()) {

    @Test
    fun `migrerer sammenligningsgrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/214/expected.json",
            originalJson = "/migrations/214/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}