package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V171FjerneForkastedePerioderUtenSykdomstidslinjeTest : MigrationTest(V171FjerneForkastedePerioderUtenSykdomstidslinje()) {
    @Test
    fun `migrerer kilde`() {
        assertMigration(
            expectedJson = "/migrations/171/expected.json",
            originalJson = "/migrations/171/original.json"
        )
    }
}