package no.nav.helse.serde.migration

import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode
import java.time.LocalDate
import java.util.*

internal class V147LagreArbeidsforholdForOpptjeningTest : MigrationTest(V147LagreArbeidsforholdForOpptjening()) {
    private lateinit var vilkårsgrunnlag: Map<UUID, Pair<Navn, Json>>

    override fun meldingerSupplier(): MeldingerSupplier = MeldingerSupplier {
        vilkårsgrunnlag
    }

    @Test
    fun `Migrerer inn opptjening for vilkårsgrunnlag med meldingsreferanse`() {
        vilkårsgrunnlag = mapOf(
            UUID.fromString("51a874a5-8574-4a6a-a6b4-1d93ecbd7b85") to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(
                Arbeidsforhold("987654321", LocalDate.EPOCH, null)
            ))
        )

        assertMigration(
            "/migrations/147/enkelExpected.json",
            "/migrations/147/enkelOriginal.json"
        )
    }

    @Test
    fun `Finner riktig opptjeningsperiode for flere arbeidsforhold med gap`() {
        val vilkårsgrunnlagId2 = UUID.fromString("69010425-41d6-4f9c-8776-11269f0632ba")

        val arbeidsforhold2 = arrayOf(
            Arbeidsforhold("orgnummer1", LocalDate.EPOCH, 1.januar(2015)),
            Arbeidsforhold("orgnummer2", 1.februar(2015), 1.mars(2016)),
            Arbeidsforhold("orgnummer3", 2.mars(2016), null)
        )

        vilkårsgrunnlag = mapOf(
            vilkårsgrunnlagId2 to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(arbeidsforhold = arbeidsforhold2))
        )

        assertMigration(
            "/migrations/147/medFlereArbeidsforholdIOpptjeningExpected.json",
            "/migrations/147/medFlereArbeidsforholdIOpptjeningOriginal.json",
            JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `Migriere inn relevante arbeidsforhold for opptjening inn i arbeidsforholdhistorikken`() {
        val vilkårsgrunnlagId1 = UUID.fromString("51a874a5-8574-4a6a-a6b4-1d93ecbd7b85")
        val vilkårsgrunnlagId2 = UUID.fromString("69010425-41d6-4f9c-8776-11269f0632ba")

        val arbeidsforhold1 = arrayOf(
            Arbeidsforhold("987654321", 20.desember(2017), null),
            Arbeidsforhold("654321987", LocalDate.EPOCH, null)
        )

        val arbeidsforhold2 = arrayOf(
            Arbeidsforhold("987654321", 20.desember(2020), null),
            Arbeidsforhold("654321987", LocalDate.EPOCH, 19.desember(2020))
        )

        vilkårsgrunnlag = mapOf(
            vilkårsgrunnlagId1 to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(arbeidsforhold = arbeidsforhold1)),
            vilkårsgrunnlagId2 to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(arbeidsforhold = arbeidsforhold2))
        )

        assertMigration(
            "/migrations/147/relevantArbeidsforholdForOpptjeningIArbeidsforholdhistorikkenEksisterendeArbeidsgiverExpected.json",
            "/migrations/147/relevantArbeidsforholdForOpptjeningIArbeidsforholdhistorikkenEksisterendeArbeidsgiverOriginal.json",
            JSONCompareMode.LENIENT
        )
    }

    @Language("JSON")
    private fun vilkårsgrunnlag(
        vararg arbeidsforhold: Arbeidsforhold
    ) = """
        {
            "@opprettet": "2020-01-01T12:00:00.000000000",
            "organisasjonsnummer": "987654321",
            "@løsning": {
                "ArbeidsforholdV2": [
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
        fun toJson() = serdeObjectMapper.writeValueAsString(
            mapOf(
                "orgnummer" to orgnummer,
                "ansattSiden" to ansattSiden,
                "ansattTil" to ansattTom
            )
        )
    }

    private fun Array<out Arbeidsforhold>.toJson() = joinToString(",") { it.toJson() }
}
