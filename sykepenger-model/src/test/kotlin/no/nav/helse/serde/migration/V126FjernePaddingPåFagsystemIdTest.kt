package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V126FjernePaddingPåFagsystemIdTest : MigrationTest(V126FjernePaddingPåFagsystemId()) {

    @Test
    fun `fjerner padding på fagsystemIder som har padding`() {
        assertMigration(
            expectedJson = "/migrations/126/expected.json",
            originalJson = "/migrations/126/original.json"
        )
    }
}
