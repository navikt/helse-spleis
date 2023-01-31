package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V215TarBortInnslagFraInntektshistorikkenTest: MigrationTest(V215TarBortInnslagFraInntektshistorikken())  {

    @Test
    fun `migrerer inntektshistorikken`() {
        assertMigration(
            expectedJson = "/migrations/215/expected.json",
            originalJson = "/migrations/215/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }
}