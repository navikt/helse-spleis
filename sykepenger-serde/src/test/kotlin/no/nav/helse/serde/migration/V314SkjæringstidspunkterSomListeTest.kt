package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V314SkjæringstidspunkterSomListeTest : MigrationTest(V314SkjæringstidspunkterSomListe()) {

    @Test
    fun `migrerer skjæringstidspunkter`() {
        assertMigration("/migrations/314/expected.json", "/migrations/314/original.json")
    }
}
