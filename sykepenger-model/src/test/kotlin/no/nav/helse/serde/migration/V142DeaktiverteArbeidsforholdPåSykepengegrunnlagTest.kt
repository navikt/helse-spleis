package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V142DeaktiverteArbeidsforholdPåSykepengegrunnlagTest : MigrationTest(V142DeaktiverteArbeidsforholdPåSykepengegrunnlag()) {
    @Test
    fun `legger inn deaktiverte arbeidsforhold på sykepengegrunnlag`() {
        assertMigration(
            "/migrations/142/expected.json",
            "/migrations/142/original.json"
        )
    }
}
