package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V216FlytteHendelseIdOgTidsstempelTilSkattSykepengegrunnlagTest: MigrationTest(V216FlytteHendelseIdOgTidsstempelTilSkattSykepengegrunnlag()) {

    @Test
    fun `migrerer skattsykepengegrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/216/expected.json",
            originalJson = "/migrations/216/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}