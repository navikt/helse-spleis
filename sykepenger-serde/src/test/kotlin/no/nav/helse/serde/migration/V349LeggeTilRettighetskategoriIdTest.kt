package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V349LeggeTilRettighetskategoriIdTest : MigrationTest(V349LeggeTilRettighetskategoriId()) {
    @Test
    fun `Legger på deterministisk rettighetskategoriId på vilkårsgrunnlag fra Spleis og Infotrygd`() {
        assertMigration("/migrations/349/expected.json", "/migrations/349/original.json")
    }
}
