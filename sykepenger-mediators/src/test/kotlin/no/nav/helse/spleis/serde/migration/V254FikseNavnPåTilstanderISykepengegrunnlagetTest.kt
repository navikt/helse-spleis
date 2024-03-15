package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V254FikseNavnPåTilstanderISykepengegrunnlagetTest : MigrationTest(V254FikseNavnPåTilstanderISykepengegrunnlaget()) {

    @Test
    fun `fikser navn på tilstander i sykepengegrunnlaget`() {
        assertMigration("/migrations/254/expected.json", "/migrations/254/original.json")
    }
}