package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V315AuuerSomIkkeRapportertInntektTest : MigrationTest(V315AuuerSomIkkeRapportertInntekt()) {

    @Test
    fun `migrerer vilkårsgrunnlag`() {
        assertMigration("/migrations/315/expected.json", "/migrations/315/original.json")
    }
}
