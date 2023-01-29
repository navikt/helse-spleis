package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V212FjerneRapporterteInntekterFraInntektshistorikkTest: MigrationTest(V212FjerneRapporterteInntekterFraInntektshistorikk()) {

    @Test
    fun `migrerer sammenligningsgrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/212/expected.json",
            originalJson = "/migrations/212/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}