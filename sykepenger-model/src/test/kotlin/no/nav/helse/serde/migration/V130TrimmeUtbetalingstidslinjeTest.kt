package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V130TrimmeUtbetalingstidslinjeTest : MigrationTest(V130TrimmeUtbetalingstidslinje()) {

    @Test
    fun `trimmer innledende arbeidsdager og fridager i helg`() {
        assertMigration(
            expectedJson = "/migrations/130/expected.json",
            originalJson = "/migrations/130/original.json"
        )
    }
}
