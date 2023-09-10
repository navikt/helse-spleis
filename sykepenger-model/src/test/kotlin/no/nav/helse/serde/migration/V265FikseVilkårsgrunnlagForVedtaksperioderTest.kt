package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V265FikseVilkårsgrunnlagForVedtaksperioderTest: MigrationTest(V265FikseVilkårsgrunnlagForVedtaksperioder()) {

    @Test
    fun `fikser vilkårsgrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/265/expected.json",
            originalJson = "/migrations/265/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }

    @Test
    fun `velger vilkårsgrunnlag med samme skjæringstidspunkt før vilkårsgrunnlagId`() {
        assertMigration(
            expectedJson = "/migrations/265/expected-samme_skjæringstidspunkt.json",
            originalJson = "/migrations/265/original-samme_skjæringstidspunkt.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}