package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V304FjerneArbeidsledigSykmeldingsperioderTest : MigrationTest(V304FjerneArbeidsledigSykmeldingsperioder()) {

    @Test
    fun `fjerner sykmeldingsperioder på arbeidsledig`() {
        assertMigration("/migrations/304/expected.json", "/migrations/304/original.json")
    }

    @Test
    fun `en person som ikke har arbeidsledig skjer det ingenting med`() {
        assertMigration("/migrations/304/expected_uten_arbeidsledig.json", "/migrations/304/original_uten_arbeidsledig.json")
    }
}