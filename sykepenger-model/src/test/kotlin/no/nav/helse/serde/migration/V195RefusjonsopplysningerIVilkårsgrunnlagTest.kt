package no.nav.helse.serde.migration

import no.nav.helse.readResource
import org.junit.jupiter.api.Test

internal class V195RefusjonsopplysningerIVilkårsgrunnlagTest : MigrationTest(V195RefusjonsopplysningerIVilkårsgrunnlag()) {

    @Test
    fun `en arbeidsgiver, en refusjonsopplysning & ett vilkårsgrunnlag fra Spleis`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/195/original.json",
            expectedJson = "/migrations/195/expected.json",
        )
    }

    @Test
    fun `en arbeidsgiver, en refusjonsopplysning & ett vilkårsgrunnlag fra Infotrygd - men startskudd 19 dager før skjæringstidspunkt`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/195/finner-19-dager-før-skjæringstidspunkt_original.json",
            expectedJson = "/migrations/195/finner-19-dager-før-skjæringstidspunkt_expected.json",
        )
    }

    @Test
    fun `Defaulter til inntekt om vi ikke finner refusjonsopplysninger i refusjonshistorikken`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/195/ingen-match-i-refusjonshistorikk_original.json",
            expectedJson = "/migrations/195/ingen-match-i-refusjonshistorikk_expected.json",
        )
    }

    @Test
    fun `Defaulter til ingen refusjonsopplysninger om refusjonshistorikken er tom for Spleis-vilkårsgrunnlag`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/195/tom-refusjonshistorikk-spleis_original.json",
            expectedJson = "/migrations/195/tom-refusjonshistorikk-spleis_expected.json",
        )
    }

    @Test
    fun `Defaulter til inntekt om refusjonshistorikken er tom for Infotrygd-vilkårsgrunnlag`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/195/tom-refusjonshistorikk-infotrygd_original.json",
            expectedJson = "/migrations/195/tom-refusjonshistorikk-infotrygd_expected.json",
        )
    }

    @Test
    fun `Defaulter til ingen refusjonsopplysninger om inntektsopplysningen er skatt`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/195/skatteopplysninger_original.json",
            expectedJson = "/migrations/195/skatteopplysninger_expected.json",
        )
    }

    @Test
    fun `Defaulter ingen refusjonsopplysninger om inntektsopplysningen er 'ikke rapportert'`() {
        assertVilkårsgrunnlagMedRefusjonsopplysninger(
            originalJson = "/migrations/195/ikke-rapportert_original.json",
            expectedJson = "/migrations/195/ikke-rapportert_expected.json",
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