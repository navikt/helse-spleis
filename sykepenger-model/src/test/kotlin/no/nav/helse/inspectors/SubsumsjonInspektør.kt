package no.nav.helse.inspectors

import no.nav.helse.person.*
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.*
import no.nav.helse.person.etterlevelse.SubsumsjonVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import java.time.LocalDate
import java.util.*


internal class SubsumsjonInspektør(jurist: MaskinellJurist) : SubsumsjonVisitor {

    private val subsumsjoner = mutableListOf<Subsumsjon>()

    private data class Subsumsjon(
        val paragraf: Paragraf,
        val ledd: Ledd,
        val punktum: List<Punktum>,
        val bokstaver: List<Bokstav>,
        val versjon: LocalDate,
        val sporing: Map<String, String>,
        val utfall: Utfall,
        val input: Map<String, Any>,
        val output: Map<String, Any>
    ) {
        fun vedtaksperiodeIdFraSporing(): UUID = UUID.fromString(sporing["vedtaksperiode"])
    }

    init {
        jurist.subsumsjoner().forEach { it.accept(this) }
    }

    private fun finnSubsumsjon(paragraf: Paragraf, versjon: LocalDate?, ledd: Ledd?, punktum: List<Punktum>?, bokstav: List<Bokstav>?, utfall: Utfall? = null, vedtaksperiodeId: UUID? = null) =
        subsumsjoner.filter {
            it.paragraf == paragraf
                && versjon?.equals(it.versjon) ?: true
                && utfall?.equals(it.utfall) ?: true
                && ledd?.equals(it.ledd) ?: true
                && punktum?.equals(it.punktum) ?: true
                && bokstav?.equals(it.bokstaver) ?: true
                && vedtaksperiodeId?.equals(it.vedtaksperiodeIdFraSporing()) ?: true
        }.let {
            assertEquals(1, it.size, "Forventer en, og kun en subsumsjon for vilkåret. Subsumsjoner funnet: $it")
            it.first()
        }

    internal fun assertBeregnet(
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd,
        punktum: List<Punktum> = emptyList(),
        bokstaver: List<Bokstav> = emptyList(),
        input: Map<String, Any>,
        output: Map<String, Any>,
    ) {
        val resultat = finnSubsumsjon(paragraf, versjon, ledd, punktum, bokstaver, VILKAR_BEREGNET)
        assertEquals(VILKAR_BEREGNET, resultat.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
        assertResultat(input, output, resultat)
    }

    internal fun assertOppfylt(
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd,
        punktum: List<Punktum> = emptyList(),
        bokstaver: List<Bokstav> = emptyList(),
        input: Map<String, Any>,
        output: Map<String, Any>,
    ) {
        val resultat = finnSubsumsjon(paragraf, versjon, ledd, punktum, bokstaver, VILKAR_OPPFYLT)
        assertEquals(VILKAR_OPPFYLT, resultat.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
        assertResultat(input, output, resultat)
    }

    internal fun assertIkkeOppfylt(
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd,
        punktum: List<Punktum> = emptyList(),
        bokstaver: List<Bokstav> = emptyList(),
        input: Map<String, Any>,
        output: Map<String, Any>,
    ) {
        val resultat = finnSubsumsjon(paragraf, versjon, ledd, punktum, bokstaver, VILKAR_IKKE_OPPFYLT)
        assertEquals(VILKAR_IKKE_OPPFYLT, resultat.utfall) { "Forventet ikke oppfylt $paragraf $ledd $punktum" }
        assertResultat(input, output, resultat)
    }

    internal fun assertVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: List<Punktum>? = null,
        bokstaver: List<Bokstav>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        versjon: LocalDate? = null,
        organisasjonsnummer: String = AbstractPersonTest.ORGNUMMER
    ) {
        assertDoesNotThrow("Forventet at $paragraf $ledd $punktum er vurdert") {
            finnSubsumsjon(paragraf, versjon, ledd, punktum, bokstaver, null, vedtaksperiodeId?.id(organisasjonsnummer))
        }
    }

    internal fun assertIkkeVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: List<Punktum>? = null,
        bokstav: List<Bokstav>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        versjon: LocalDate? = null,
        organisasjonsnummer: String = AbstractPersonTest.ORGNUMMER
    ) {
        assertThrows<AssertionFailedError> {
            finnSubsumsjon(paragraf, versjon, ledd, punktum, bokstav, null, vedtaksperiodeId?.id(organisasjonsnummer))
        }
    }

    internal fun assertParagraf(
        expectedParagraf: Paragraf,
        expectedLedd: Ledd,
        expectedVersjon: LocalDate,
        expectedPunktum: List<Punktum> = emptyList(),
        expectedBokstaver: List<Bokstav> = emptyList(),
        utfall: Utfall? = null
    ) {
        finnSubsumsjon(expectedParagraf, expectedVersjon, expectedLedd, expectedPunktum, expectedBokstaver, utfall)
    }

    private fun assertResultat(inputdata: Map<String, Any>, outputdata: Map<String, Any>, resultat: Subsumsjon) {
        assertEquals(inputdata, resultat.input)
        assertEquals(outputdata, resultat.output)
    }

    override fun preVisitSubsumsjon(
        utfall: Utfall,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav>,
        input: Map<String, Any>,
        output: Map<String, Any>,
        kontekster: Map<String, String>
    ) {
        subsumsjoner.add(Subsumsjon(paragraf, ledd, punktum, bokstaver, versjon, kontekster, utfall, input, output))
    }
}
