package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V268ForkasteVilkårsgrunnlagUtenInntekterTest: MigrationTest(V268ForkasteVilkårsgrunnlagUtenInntekter()) {

    @Test
    fun `forkaster vilkårsgrunnlag`() {
        assertMigration("/migrations/268/expected.json", "/migrations/268/original.json", JSONCompareMode.STRICT_ORDER)
    }

}