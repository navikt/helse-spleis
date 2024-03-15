package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V289AvsluttetTidspunktForkastedeGenerasjonerTest: MigrationTest(V289AvsluttetTidspunktForkastedeGenerasjoner()) {

    @Test
    fun `setter avsluttettidspunkt`() {
        assertMigration("/migrations/289/expected.json", "/migrations/289/original.json")
    }
}