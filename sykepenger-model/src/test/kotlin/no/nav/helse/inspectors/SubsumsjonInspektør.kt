package no.nav.helse.inspectors

import no.nav.helse.person.*
import no.nav.helse.person.AbstractPersonTest.Companion.ORGNUMMER
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.MaskinellJurist.KontekstType
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.*
import no.nav.helse.person.etterlevelse.SubsumsjonVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.util.*


internal class SubsumsjonInspektør(jurist: MaskinellJurist) : SubsumsjonVisitor {

    private val subsumsjoner = mutableListOf<Subsumsjon>()

    private data class Subsumsjon(
        val paragraf: Paragraf,
        val ledd: Ledd?,
        val punktum: Punktum?,
        val bokstav: Bokstav?,
        val versjon: LocalDate,
        val sporing: Map<String, KontekstType>,
        val utfall: Utfall,
        val input: Map<String, Any>,
        val output: Map<String, Any>
    ) {
        fun vedtaksperiodeIdFraSporing(): UUID = UUID.fromString(sporing.filter { it.value == KontekstType.Vedtaksperiode }.keys.first())
    }

    init {
        jurist.subsumsjoner().forEach { it.accept(this) }
    }

    private fun finnSubsumsjoner(
        paragraf: Paragraf,
        versjon: LocalDate?,
        ledd: Ledd?,
        punktum: Punktum?,
        bokstav: Bokstav?,
        utfall: Utfall? = null,
        vedtaksperiodeId: UUID? = null
    ) =
        subsumsjoner.filter {
            it.paragraf == paragraf
                && versjon?.equals(it.versjon) ?: true
                && utfall?.equals(it.utfall) ?: true
                && ledd?.equals(it.ledd) ?: true
                && punktum?.equals(it.punktum) ?: true
                && bokstav?.equals(it.bokstav) ?: true
                && vedtaksperiodeId?.equals(it.vedtaksperiodeIdFraSporing()) ?: true
        }

    internal fun assertBeregnet(
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd?,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>,
        output: Map<String, Any>,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat = finnSubsumsjoner(paragraf, versjon, ledd, punktum, bokstav, VILKAR_BEREGNET, vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat.first()
        assertEquals(VILKAR_BEREGNET, subsumsjon.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertOppfylt(
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>? = null,
        output: Map<String, Any>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat = finnSubsumsjoner(paragraf, versjon, ledd, punktum, bokstav, VILKAR_OPPFYLT, vedtaksperiodeId = vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat.first()
        assertEquals(VILKAR_OPPFYLT, subsumsjon.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
        if (input == null && output == null) return
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertIkkeOppfylt(
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>? = null,
        output: Map<String, Any>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat =
            finnSubsumsjoner(paragraf, versjon, ledd, punktum, bokstav, VILKAR_IKKE_OPPFYLT, vedtaksperiodeId = vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat.first()
        assertEquals(VILKAR_IKKE_OPPFYLT, subsumsjon.utfall) { "Forventet ikke oppfylt $paragraf $ledd $punktum" }
        if (input == null && output == null) return
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertFlereIkkeOppfylt(
        antall: Int,
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>,
        output: Map<String, Any>,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat = subsumsjoner.filter {
            it.paragraf == paragraf
                && (versjon == it.versjon)
                && (VILKAR_IKKE_OPPFYLT == it.utfall)
                && (ledd == it.ledd)
                && punktum?.equals(it.punktum) ?: true
                && bokstav?.equals(it.bokstav) ?: true
                && (input == it.input)
                && (output == it.output)
                && vedtaksperiodeId?.id(organisasjonsnummer)?.equals(it.vedtaksperiodeIdFraSporing()) ?: true
        }.let {
            assertEquals(antall, it.size, "Forventer $antall subsumsjoner for vilkåret. Subsumsjoner funnet: $it")
            it
        }
        resultat.forEach {
            assertEquals(VILKAR_IKKE_OPPFYLT, it.utfall) { "Forventet ikke oppfylt $paragraf $ledd $punktum" }
            assertResultat(input, output, it)
        }
    }

    internal fun assertVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        versjon: LocalDate? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat = finnSubsumsjoner(paragraf, versjon, ledd, punktum, bokstav, null, vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
    }

    internal fun assertIkkeVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        versjon: LocalDate? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat = finnSubsumsjoner(paragraf, versjon, ledd, punktum, bokstav, null, vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(0, resultat.size, "Forventer ingen subsumsjoner. Subsumsjoner funnet: $resultat")
    }

    private fun assertResultat(inputdata: Map<String, Any>?, outputdata: Map<String, Any>?, resultat: Subsumsjon) {
        assertEquals(inputdata, resultat.input)
        assertEquals(outputdata, resultat.output)
    }

    override fun preVisitSubsumsjon(
        utfall: Utfall,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd?,
        punktum: Punktum?,
        bokstav: Bokstav?,
        input: Map<String, Any>,
        output: Map<String, Any>,
        kontekster: Map<String, KontekstType>
    ) {
        subsumsjoner.add(Subsumsjon(paragraf, ledd, punktum, bokstav, versjon, kontekster, utfall, input, output))
    }
}
