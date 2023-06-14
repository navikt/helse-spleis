package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V253SykepengegrunnlagtilstandTest: MigrationTest(V253Sykepengegrunnlagtilstand()) {

    @Test
    fun `setter tilstand`() {
        assertMigration("/migrations/253/expected.json", "/migrations/253/original.json")
    }
}