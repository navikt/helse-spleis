package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V283BeregningsgrunnlagPåØkonomiTest: MigrationTest(V283BeregningsgrunnlagPåØkonomi()) {

    @Test
    fun `migrerer inn beregningsgrunnlag`() {
        assertMigration("/migrations/283/expected.json", "/migrations/283/original.json")
    }
}