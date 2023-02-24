package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V231NyttTilstandsnavnAvventerInntektsmeldingTest: MigrationTest(V231NyttTilstandsnavnAvventerInntektsmelding()) {

    @Test
    fun `endrer tilstand AvventerInntektsmelding`() {
        assertMigration(
            expectedJson = "/migrations/231/expected.json",
            originalJson = "/migrations/231/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}