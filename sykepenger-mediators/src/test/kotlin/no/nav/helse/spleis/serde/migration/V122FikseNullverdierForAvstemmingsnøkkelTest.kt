package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V122FikseNullverdierForAvstemmingsnøkkelTest : MigrationTest(V122FikseNullverdierForAvstemmingsnøkkel()) {

    @Test
    fun `setter string null til å være ekte null`() {
        assertMigration(
            expectedJson = "/migrations/122/expected.json",
            originalJson = "/migrations/122/original.json"
        )
    }
}
