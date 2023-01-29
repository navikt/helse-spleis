package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V213FlytteDatoTilSkattSykepengegrunnlagTest: MigrationTest(V213FlytteDatoTilSkattSykepengegrunnlag()) {

    @Test
    fun `migrerer skattsykepengegrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/213/expected.json",
            originalJson = "/migrations/213/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}