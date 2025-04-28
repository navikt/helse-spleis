package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V322YrkesaktivitetstypePåArbeidsgiverTest : MigrationTest(V322YrkesaktivitetstypePåArbeidsgiver()) {
    val expectedJson = "/migrations/322/expected.json"
    val originalJson = "/migrations/322/original.json"

    @Test
    fun `yrkesaktivitetstype på arbeidsgiver`() {
        assertMigration(expectedJson = expectedJson, originalJson = originalJson)
    }
}
