package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V318FjerneSykNavSomSykdomstidslinjedagtypeTest : MigrationTest(V318FjerneSykNavSomSykdomstidslinjedagtype()) {

    @Test
    fun `migrerer syknav`() {
        assertMigration("/migrations/318/expected.json", "/migrations/318/original.json")
    }
}
