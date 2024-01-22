package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V285LoggeRareAnnulleringerTest: MigrationTest(V285LoggeRareAnnulleringer(forkast = setOf("52fc8116-1757-45bc-8ca1-d333b33d3496"))) {

    @Test
    fun `forkaster annulleringer som ikke er sendt til oppdrag`() {
        assertMigration("/migrations/285/expected.json", "/migrations/285/original.json")
    }
}