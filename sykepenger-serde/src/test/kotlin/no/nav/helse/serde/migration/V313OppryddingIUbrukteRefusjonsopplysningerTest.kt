package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V313OppryddingIUbrukteRefusjonsopplysningerTest : MigrationTest(V313OppryddingIUbrukteRefusjonsopplysninger()) {


    @Test
    fun `migrerer bort gamle opplysninger før siste vedtaksperiode`() {
        assertMigration("/migrations/313/expected.json", "/migrations/313/original.json")
    }

}