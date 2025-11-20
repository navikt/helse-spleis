package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V342RenameAvventersøknadForTidligereEllerOverlappendePeriodeTest: MigrationTest(V342RenameAvventersøknadForTidligereEllerOverlappendePeriode()) {

    @Test
    fun `Renamer tilstand AVVENTER_SØKNAD_FOR_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODE til AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE`() {
        assertMigration("/migrations/342/expected.json", "/migrations/342/original.json")
    }
}
