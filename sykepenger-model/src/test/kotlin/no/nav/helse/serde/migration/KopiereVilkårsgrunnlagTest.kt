package no.nav.helse.serde.migration

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.readResource
import org.junit.jupiter.api.Test

internal class KopiereVilkårsgrunnlagTest: MigrationTest(TestMigration) {

    @Test
    fun `ikke relevant person uten vilkårsgrunnlag`() = assertKopierteVilkårgrunnlag(
        originalJson = "/migrations/kopierevilkårsgrunnlag/ikke-relevant-uten-vilkårsgrunnlag_original.json",
        expectedJson = "/migrations/kopierevilkårsgrunnlag/ikke-relevant-uten-vilkårsgrunnlag_expected.json",
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
        UUID.fromString("33fa499a-6fd2-43c2-a9cf-bdde739c546b") to LocalDate.parse("2020-05-05"),
        UUID.fromString("491f6fb1-7fea-4fec-8095-1a0f8f7d543b") to LocalDate.parse("2020-01-01"),
        UUID.fromString("dc3004ae-6d36-4f11-bb2c-32245540fb0e") to LocalDate.parse("2021-12-31")
    )
}