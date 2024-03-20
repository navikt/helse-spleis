package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V290FikseForbrukteDagerSomErNullTest: MigrationTest(V290FikseForbrukteDagerSomErNull()) {

    @Test
    fun `setter forbrukte og gjenstående dager til 0`() {
        assertMigration("/migrations/290/expected.json", "/migrations/290/original.json")
    }
}