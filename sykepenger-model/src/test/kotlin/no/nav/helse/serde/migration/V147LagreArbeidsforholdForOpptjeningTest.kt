package no.nav.helse.serde.migration

import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
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
    fun `migrerer opptjening fra vilkårsgrunnlagmelding med gammelt behovsnavn`() {
        val arbeidsforhold = arrayOf(
            Arbeidsforhold("987654321", LocalDate.EPOCH, null)
        )

        val vilkårsgrunnlagId = UUID.fromString("51a874a5-8574-4a6a-a6b4-1d93ecbd7b85")
        vilkårsgrunnlag = mapOf(
            vilkårsgrunnlagId to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlagMedGammeltNavnPåLøsning(*arbeidsforhold))
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
            vilkårsgrunnlagId2 to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(*arbeidsforhold2))
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
            vilkårsgrunnlagId1 to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(*arbeidsforhold1)),
            vilkårsgrunnlagId2 to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(*arbeidsforhold2))
        )

        assertMigration(
            "/migrations/147/relevantArbeidsforholdForOpptjeningIArbeidsforholdhistorikkenEksisterendeArbeidsgiverExpected.json",
            "/migrations/147/relevantArbeidsforholdForOpptjeningIArbeidsforholdhistorikkenEksisterendeArbeidsgiverOriginal.json",
            JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `en arbeidsgiver med flere opptjeningsperioder for samme orgnummer`() {
        val vilkårsgrunnlagId = UUID.fromString("ba640e1e-5fac-4e61-9b0f-52aa4f819702")
        val arbeidsforhold = arrayOf(
            Arbeidsforhold("987654321", LocalDate.EPOCH, 1.desember(2017)),
            Arbeidsforhold("987654321", 1.november(2017), null)
        )

        vilkårsgrunnlag = mapOf(
            vilkårsgrunnlagId to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(*arbeidsforhold))
        )

        assertMigration(
            "/migrations/147/enArbeidsgiverFlereOpptjeningsperioderExpected.json",
            "/migrations/147/enArbeidsgiverFlereOpptjeningsperioderOriginal.json"
        )
    }

    @Test
    fun `kopierer opptjening for vilkårsgrunnlag med revurdert inntekt`() {
        val vilkårsgrunnlagId = UUID.fromString("247d1c71-2254-4efc-8900-d7c76f77e027")
        val arbeidsforhold = arrayOf(
            Arbeidsforhold("987654321", LocalDate.EPOCH, null)
        )

        vilkårsgrunnlag = mapOf(
            vilkårsgrunnlagId to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(*arbeidsforhold))
        )
        assertMigration(
            "/migrations/147/enkelPersonMedRevurdertInntektExpected.json",
            "/migrations/147/enkelPersonMedRevurdertInntektOriginal.json"
        )
    }

    @Test
    fun `kopierer opptjening for vilkårsgrunnlag med lik opptjening og ulikt skjæringstidspunkt`() {
        val vilkårsgrunnlagId = UUID.fromString("247d1c71-2254-4efc-8900-d7c76f77e027")
        val arbeidsforhold = arrayOf(
            Arbeidsforhold("987654321", LocalDate.EPOCH, null)
        )

        vilkårsgrunnlag = mapOf(
            vilkårsgrunnlagId to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(*arbeidsforhold))
        )
        assertMigration(
            "/migrations/147/enkelPersonMedLignendeOpptjeningUliktSkjæringstidspunktExpected.json",
            "/migrations/147/enkelPersonMedLignendeOpptjeningUliktSkjæringstidspunktOriginal.json"
        )
    }

    @Test
    fun `lager dummy-opptjening ved manglende kobling`() {
        vilkårsgrunnlag = emptyMap()
        assertMigration(
            "/migrations/147/ingenKoblingTilVilkårsgrunnlagsmeldingExpected.json",
            "/migrations/147/ingenKoblingTilVilkårsgrunnlagsmeldingOriginal.json"
        )
    }

    @Test
    fun `kopiere ikke opptjening ved ulik sammenligningsgrunnlag og skjæringstidspunkt`() {
        val meldingsreferanseId = UUID.fromString("0e0dd2b2-9b4f-4d42-86d9-77668fe6f7c9")
        val arbeidsforhold = arrayOf(
            Arbeidsforhold("987654321", 1.desember(2018), null)
        )

        vilkårsgrunnlag = mapOf(
            meldingsreferanseId to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(*arbeidsforhold))
        )

        assertMigration(
            "/migrations/147/vilkårsgrunnlagMedUliktSammelingsgrunnlagOgSkjæringstidspunktExpected.json",
            "/migrations/147/vilkårsgrunnlagMedUliktSammelingsgrunnlagOgSkjæringstidspunktOriginal.json"
        )
    }

    @Test
    fun `kopiere opptjening ved minimalt med felter`() {
        val arbeidsforhold = arrayOf(
            Arbeidsforhold("987654321", LocalDate.EPOCH, null)
        )

        val vilkårsgrunnlagId = UUID.fromString("51a874a5-8574-4a6a-a6b4-1d93ecbd7b85")
        vilkårsgrunnlag = mapOf(
            vilkårsgrunnlagId to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlagMedGammeltNavnPåLøsning(*arbeidsforhold))
        )

        assertMigration(
            "/migrations/147/personMedNullSomArbeidsForholdExpected.json",
            "/migrations/147/personMedNullSomArbeidsforholdOriginal.json",
            JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `mangler antallOpptjeningsdagerErMinst`() {
        vilkårsgrunnlag = emptyMap()

        assertMigration(
            "/migrations/147/manglendeAntallOpptjeningsdagerErMinstExpected.json",
            "/migrations/147/manglendeAntallOpptjeningsdagerErMinstOriginal.json"
        )
    }

    @Test
    fun `arbeidsforhold som startet etter skjæringstidspunkt`() {
        vilkårsgrunnlag = mapOf(
            UUID.fromString("51a874a5-8574-4a6a-a6b4-1d93ecbd7b85") to ("VILKÅRSGRUNNLAG" to vilkårsgrunnlag(
                Arbeidsforhold("987654321", LocalDate.EPOCH, null),
                Arbeidsforhold("654321987", 1.februar, null)
            ))
        )

        assertMigration(
            "/migrations/147/enkelExpected.json",
            "/migrations/147/enkelOriginal.json"
        )
    }

    @Language("JSON")
    private fun vilkårsgrunnlag(
        vararg arbeidsforhold: Arbeidsforhold,
    ) = """
        {
            "@opprettet": "2020-01-01T12:00:00.000000000",
            "@id": "cdab7587-4be7-46a8-a39d-4445719e59b1",
            "fødselsnummer": "FNR",
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

    @Language("JSON")
    private fun vilkårsgrunnlagMedGammeltNavnPåLøsning(
        vararg arbeidsforhold: Arbeidsforhold,
    ) = """
        {
            "@opprettet": "2020-01-01T12:00:00.000000000",
            "@id": "cdab7587-4be7-46a8-a39d-4445719e59b1",
            "fødselsnummer": "FNR",
            "organisasjonsnummer": "987654321",
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
