package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V202NullstilleSisteArbeidsgiverdagTest : MigrationTest(V202NullstilleSisteArbeidsgiverdag()) {
    @Test
    fun `migrer bort fra LocalDateMIN`() {
        assertMigration(
            expectedJson = "/migrations/202/expected.json",
            originalJson = "/migrations/202/original.json"
        )
    }
}