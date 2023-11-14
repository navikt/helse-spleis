package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V284GjelderPeriodeArbeidsgiverInntektsopplysningTest: MigrationTest(V284GjelderPeriodeArbeidsgiverInntektsopplysning()) {

    @Test
    fun `migrerer inn fom og tom`() {
        assertMigration("/migrations/284/expected.json", "/migrations/284/original.json")
    }
}