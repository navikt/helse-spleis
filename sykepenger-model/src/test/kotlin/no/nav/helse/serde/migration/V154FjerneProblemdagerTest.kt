package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V154FjerneProblemdagerTest: MigrationTest(V154FjerneProblemdager()) {

    @Test
    fun `ingen endring på irrelevant arbeidsgiver`() {
        assertMigration(
            expectedJson = "/migrations/154/original-irrelevant.json",
            originalJson = "/migrations/154/original-irrelevant.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }

    @Test
    fun `endring på relevant arbeidsgiver`() {
        assertMigration(
            expectedJson = "/migrations/154/expected.json",
            originalJson = "/migrations/154/original.json"
        )
    }
}