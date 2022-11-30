package no.nav.helse.serde.migration

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.readResource
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Test

internal class KopiereVilkårsgrunnlagTest: MigrationTest(TestMigration) {

    @Test
    fun `ikke relevant person uten vilkårsgrunnlag`() = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/ikke-relevant-uten-vilkårsgrunnlag_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/ikke-relevant-uten-vilkårsgrunnlag_expected.json",
    )

    @Test
    fun `ikke relevant person med vilkårsgrunnlag`() = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/ikke-relevant-med-vilkårsgrunnlag_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/ikke-relevant-med-vilkårsgrunnlag_expected.json",
    )

    @Test
    fun `relevant person som skal få kopiert ett vilkårsgrunnlag`() = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/relevant-person_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/relevant-person_expected.json",
    )

    @Test
    fun `relevant person med flere vilkårsgrunnlag som skal kopieres`() = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/relevant-person-flere-skjæringstidspunkt_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/relevant-person-flere-skjæringstidspunkt_expected.json",
    )
    @Test
    fun `relevant person kopierer vilkårsgrunnlag på fler skjæringstidspunkt`() = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/relevant-person-kopiere-vilkårsgrunnlag-på-fler-skjæringstidspunkt_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/relevant-person-kopiere-vilkårsgrunnlag-på-fler-skjæringstidspunkt_expected.json",
    )

    @Test
    fun `kopierer ikke vilkårsgrunnlag om personen allerede har vilkårsgrunnlag på skjæringstidspunktet`()  = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/ikke-kopier-om-skjæringstidspunkt-har-vilkårsgrunnlag_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/ikke-kopier-om-skjæringstidspunkt-har-vilkårsgrunnlag_expected.json",
    )

    @Test
    fun `kopierer vilkårsgrunnlag om personen allerede har vilkårsgrunnlag på skjæringstidspunktet`()  = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/kopier-om-skjæringstidspunkt-har-vilkårsgrunnlag-eldre-innslag_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/kopier-om-skjæringstidspunkt-har-vilkårsgrunnlag-eldre-innslag_expected.json",
    )

    @Test
    fun `kopiere inn med nye refusjonsopplysninger`() = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/kopier-med-nye-refusjonsopplysninger_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/kopier-med-nye-refusjonsopplysninger_expected.json",
    )
    private fun assertKopierteVilkårgrunnlag(originalJson: String, expectedJson: String) {
        val migrert = migrer(originalJson.readResource())
        val sisteInnslag = migrert.path("vilkårsgrunnlagHistorikk").firstOrNull()
        val expected = expectedJson.readResource()
            .replace("{id}", sisteInnslag?.path("id")?.asText() ?: "")
            .replace("{opprettet}", sisteInnslag?.path("opprettet")?.asText() ?: "")
            .let { json ->
                var tmpJson = json
                sisteInnslag?.path("vilkårsgrunnlag")?.forEachIndexed { index, jsonNode ->
                    tmpJson = tmpJson.replace("{nyVilkårsgrunnlagId${index}}", jsonNode.path("vilkårsgrunnlagId").asText())
                }
                tmpJson
            }
        assertJson(migrert.toString(), expected)
    }


    private object TestMigration: KopiereVilkårsgrunnlag(
        versjon = 1337,
        Triple(UUID.fromString("33fa499a-6fd2-43c2-a9cf-bdde739c546b"), LocalDate.parse("2020-05-05"), null),
        Triple(UUID.fromString("491f6fb1-7fea-4fec-8095-1a0f8f7d543b"), LocalDate.parse("2020-01-01"), null),
        Triple(UUID.fromString("dc3004ae-6d36-4f11-bb2c-32245540fb0e"), LocalDate.parse("2021-12-31"), null),
        Triple(UUID.fromString("4284fe36-78ed-4191-b12e-caccaabe5962"), LocalDate.parse("2020-05-05"), null),
        Triple(UUID.fromString("4284fe36-78ed-4191-b12e-caccaabe5962"), LocalDate.parse("2021-05-05"), null),
        Triple(UUID.fromString("826f09da-f8e2-459b-8326-5ee8d10aa9e3"), LocalDate.parse("2018-01-01"), null),
        Triple(UUID.fromString("0473f5e9-092e-4ff5-a321-23cd5e9330aa"), LocalDate.parse("2018-03-01"), null),
        Triple(UUID.fromString("893e57bf-6377-47bf-b029-3fb0c625f877"), LocalDate.parse("2018-05-01"),
            Refusjonsopplysning(UUID.fromString("88a37f64-947c-4e33-8e22-5162989c390c"), LocalDate.parse("2018-05-01"), null, 31000.månedlig).refusjonsopplysninger
        )
    )
}