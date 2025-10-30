package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V341KonsekventNavngivningFaktaavklartInntektPåDeaktiverteArbeidsforholdTest: MigrationTest(V341KonsekventNavngivningFaktaavklartInntektPåDeaktiverteArbeidsforhold()) {

    @Test
    fun `Konsekvent navngivning av faktaavklart inntekt for deaktiverte arbeidsforhold`() {
        assertMigration("/migrations/341/expected.json", "/migrations/341/original.json")
    }
}
