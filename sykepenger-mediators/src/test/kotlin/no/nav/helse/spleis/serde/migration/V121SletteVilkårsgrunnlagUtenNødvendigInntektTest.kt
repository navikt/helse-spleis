package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V121SletteVilkårsgrunnlagUtenNødvendigInntektTest : MigrationTest(V121SletteVilkårsgrunnlagUtenNødvendigInntekt()) {

    @Test
    fun `vilkårsgrunnlag uten nødvendig inntekt`() {
        assertMigration(
            expectedJson = "/migrations/121/personMedVilkårsgrunnlagUtenNødvendigInntektExpected.json",
            originalJson = "/migrations/121/personMedVilkårsgrunnlagUtenNødvendigInntektOriginal.json"
        )
    }
}
