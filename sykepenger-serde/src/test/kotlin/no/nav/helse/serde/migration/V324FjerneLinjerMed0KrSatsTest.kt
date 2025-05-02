package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V324FjerneLinjerMed0KrSatsTest : MigrationTest(V324FjerneLinjerMed0KrSats()) {

    @Test
    fun `fjerner linjer med 0 kr i sats`() {
        assertMigration("/migrations/324/expected.json", "/migrations/324/original.json")
    }
}
