package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class V145LagreArbeidsforholdForOpptjeningTest : MigrationTest(V145LagreArbeidsforholdForOpptjening()) {
    val vilkårsgrunnlagId1 = UUID.randomUUID()
    override fun meldingerSupplier(): MeldingerSupplier = MeldingerSupplier {
        mapOf(
            vilkårsgrunnlagId1 to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag()),
            UUID.randomUUID() to ("VILKÅRSGRUNNLAG" to gammeltVilkårsgrunnlag(UUID.randomUUID().toString()))
        )
    }

    @Test
    fun `kaster ikke exceptions`() {
        assertMigration(
            "/migrations/145/expected.json",
            "/migrations/145/original.json"
        )
    }

    @Language("JSON")
    private fun vilkårsgrunnlag(
        vedtaksperiodeId: String = "ed694052-1827-456a-9a58-4d3281eb8976",
        vararg arbeidsforhold: Arbeidsforhold = arrayOf(
            Arbeidsforhold("987654321", LocalDate.EPOCH, null)
        )
    ) = """
{
    "@opprettet": "2020-01-01T12:00:00.000000000",
    "@id": "51a874a5-8574-4a6a-a6b4-1d93ecbd7b85",
    "organisasjonsnummer": "987654321",
    "vedtaksperiodeId": "$vedtaksperiodeId",
    "@løsning": {
        "ArbeidsforholdV2": [
            ${arbeidsforhold.toJson()}
        ]
    },
    "@final": true,
    "@besvart": "2020-01-01T12:01:00.000000000"
}
    """

    @Language("JSON")
    private fun gammeltVilkårsgrunnlag(
        vedtaksperiodeId: String = "ed694052-1827-456a-9a58-4d3281eb8976",
        vararg arbeidsforhold: Arbeidsforhold = arrayOf(
            Arbeidsforhold("987654321", LocalDate.EPOCH, null)
        )
    ) = """
{
    "@opprettet": "2020-01-01T12:00:00.000000000",
    "@id": "51a874a5-8574-4a6a-a6b4-1d93ecbd7b85",
    "organisasjonsnummer": "987654321",
    "vedtaksperiodeId": "$vedtaksperiodeId",
    "@løsning": {
        "Opptjening": [
            ${arbeidsforhold.toJson()}
        ]
    },
    "@final": true,
    "@besvart": "2020-01-01T12:01:00.000000000"
}
    """

    private data class Arbeidsforhold(
        val orgnummer: String,
        val ansattSiden: LocalDate,
        val ansattTom: LocalDate?
    ) {
        fun toJson() = serdeObjectMapper.writeValueAsString(mapOf(
            "orgnummer" to orgnummer,
            "ansattSiden" to ansattSiden,
            "ansattTil" to ansattTom
        ))
    }

    private fun Array<out Arbeidsforhold>.toJson() = joinToString(",") { it.toJson() }
}
