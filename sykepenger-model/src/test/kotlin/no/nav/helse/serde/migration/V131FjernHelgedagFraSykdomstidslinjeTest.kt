package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V131FjernHelgedagFraSykdomstidslinjeTest : MigrationTest(V131FjernHelgedagFraSykdomstidslinje()) {
    @Test
    fun `Fjerner SYK_HELGEDAG og ARBEIDSGIVER_HELGEDAG fra sykdomstidslinjer`() {
        assertMigration(
            expectedJson = "/migrations/131/expected.json",
            originalJson = "/migrations/131/original.json"
        )
    }
}
