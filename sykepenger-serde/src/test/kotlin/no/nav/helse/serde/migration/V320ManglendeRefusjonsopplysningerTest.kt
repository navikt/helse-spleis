package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V320ManglendeRefusjonsopplysningerTest : MigrationTest(V320ManglendeRefusjonsopplysninger()) {

    @Test
    fun `migrerer inn manglende refusjonsopplysninger`() {
        assertMigration("/migrations/320/expected.json", "/migrations/320/original.json")
    }
}
