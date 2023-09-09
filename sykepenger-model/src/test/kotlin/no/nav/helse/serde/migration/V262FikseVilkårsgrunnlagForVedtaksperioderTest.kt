package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V262FikseVilkårsgrunnlagForVedtaksperioderTest: MigrationTest(V262FikseVilkårsgrunnlagForVedtaksperioder()) {

    @Test
    fun `fikser vilkårsgrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/262/expected.json",
            originalJson = "/migrations/262/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}