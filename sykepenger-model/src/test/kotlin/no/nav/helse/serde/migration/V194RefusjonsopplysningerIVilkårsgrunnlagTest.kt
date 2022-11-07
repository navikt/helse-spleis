package no.nav.helse.serde.migration

import no.nav.helse.readResource
import org.junit.jupiter.api.Test

internal class V194RefusjonsopplysningerIVilkårsgrunnlagTest : MigrationTest(V194RefusjonsopplysningerIVilkårsgrunnlag()) {

    @Test
    fun `en arbeidsgiver, en refusjonsopplysning & ett vilkårsgrunnlag fra Spleis`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/194/original.json",
            expectedJson = "/migrations/194/expected.json",
        )
    }

    @Test
    fun `en arbeidsgiver, en refusjonsopplysning & ett vilkårsgrunnlag fra Infotrygd - men startskudd 19 dager før skjæringstidspunkt`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/194/finner-19-dager-før-skjæringstidspunkt_original.json",
            expectedJson = "/migrations/194/finner-19-dager-før-skjæringstidspunkt_expected.json",
        )
    }

    @Test
    fun `Defaulter til inntekt om vi ikke finner refusjonsopplysninger i refusjonshistorikken`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/194/tom-refusjonshistorikk_original.json",
            expectedJson = "/migrations/194/tom-refusjonshistorikk_expected.json",
        )
    }

    private fun assertVilkårsgrunnlagMedRefusjonsopplysninger(originalJson: String, expectedJson: String) {
        val migrert = migrer(originalJson.readResource())
        val sisteInnslag = migrert.path("vilkårsgrunnlagHistorikk")[0]
        val expected = expectedJson.readResource()
            .replace("{id}", sisteInnslag.path("id").asText())
            .replace("{opprettet}", sisteInnslag.path("opprettet").asText())
        assertJson(migrert.toString(), expected)
    }
}