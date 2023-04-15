package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V240KopiereSykdomstidslinjeTilVedtaksperiodeutbetalingerTest : MigrationTest(V240KopiereSykdomstidslinjeTilVedtaksperiodeutbetalinger()) {
    @Test
    fun `migrerer sykdomstidslinjer`() {
        assertMigration(
            expectedJson = "/migrations/240/expected.json",
            originalJson = "/migrations/240/original.json"
        )
    }
}