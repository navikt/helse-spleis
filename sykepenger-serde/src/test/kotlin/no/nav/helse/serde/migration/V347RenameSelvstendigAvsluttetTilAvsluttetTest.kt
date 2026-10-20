package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V347RenameSelvstendigAvsluttetTilAvsluttetTest : MigrationTest(V347RenameSelvstendigAvsluttetTilAvsluttet()) {
    @Test
    fun `Renamer tilstand SELVSTENDIG_AVSLUTTET til AVSLUTTET`() {
        assertMigration("/migrations/347/expected.json", "/migrations/347/original.json")
    }
}
