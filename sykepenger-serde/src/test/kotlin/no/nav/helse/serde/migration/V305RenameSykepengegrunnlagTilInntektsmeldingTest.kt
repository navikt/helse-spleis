package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V305RenameSykepengegrunnlagTilInntektsmeldingTest : MigrationTest(V305RenameSykepengegrunnlagTilInntektsgrunnlag()) {

    @Test
    fun `renamer sykepengegrunnlag til inntektsgrunnlag`() {
        assertMigration("/migrations/305/expected.json", "/migrations/305/original.json")
    }
}