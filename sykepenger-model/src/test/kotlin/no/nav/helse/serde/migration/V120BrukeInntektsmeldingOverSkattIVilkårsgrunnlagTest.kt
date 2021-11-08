package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V120BrukeInntektsmeldingOverSkattIVilkårsgrunnlagTest : MigrationTest(V120BrukeInntektsmeldingOverSkattIVilkårsgrunnlag()){

    @Test
    fun `vilkårsgrunnlag bruker riktig inntektsopplysning selv om inntektsmelding sin dato ikke ligger på skjæringstidspunktet`() {
        assertMigration(
            expectedJson = "/migrations/120/personMedBareSkattExpected.json",
            originalJson = "/migrations/120/personMedBareSkattOriginal.json"
        )
    }

    @Test
    fun `vilkårsgrunnlagHistorikk fra og med 7 september skal ikke migreres`() {
        assertMigration(
            expectedJson = "/migrations/120/personMedVilkårsgrunnlagFørOgEtterV114Expected.json",
            originalJson = "/migrations/120/personMedVilkårsgrunnlagFørOgEtterV114Original.json"
        )
    }

    @Test
    fun `infotrygdvilkårsgrunnlag migreres ikke`() {
        assertMigration(
            expectedJson = "/migrations/120/personOvergangFraITExpected.json",
            originalJson = "/migrations/120/personOvergangFraITOriginal.json"
        )
    }

    @Test
    fun `person uten vilkårsgrunnlagHistorikk`() {
        assertMigration(
            expectedJson = "/migrations/120/personUtenVilkårsgrunnlagHistorikkExpected.json",
            originalJson = "/migrations/120/personUtenVilkårsgrunnlagHistorikkOriginal.json"
        )
    }
}
