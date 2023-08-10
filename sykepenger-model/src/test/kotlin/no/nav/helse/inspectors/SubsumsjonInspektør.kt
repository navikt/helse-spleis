package no.nav.helse.inspectors

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.AbstractPersonTest.Companion.ORGNUMMER
import no.nav.helse.etterlevelse.Bokstav
import no.nav.helse.person.IdInnhenter
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Punktum
import no.nav.helse.etterlevelse.KontekstType
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_BEREGNET
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.etterlevelse.SubsumsjonVisitor
import org.junit.jupiter.api.Assertions.assertEquals


internal class SubsumsjonInspektør(jurist: MaskinellJurist) : SubsumsjonVisitor {

    private val subsumsjoner = mutableListOf<Subsumsjon>()

    private data class Subsumsjon(
        val lovverk: String,
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
        lovverk: String,
        paragraf: Paragraf,
        versjon: LocalDate?,
        ledd: Ledd?,
        punktum: Punktum?,
        bokstav: Bokstav?,
        utfall: Utfall? = null,
        vedtaksperiodeId: UUID? = null
    ) =
        subsumsjoner.filter {
            lovverk == it.lovverk
                && it.paragraf == paragraf
                && versjon?.equals(it.versjon) ?: true
                && utfall?.equals(it.utfall) ?: true
                && ledd?.equals(it.ledd) ?: true
                && punktum?.equals(it.punktum) ?: true
                && bokstav?.equals(it.bokstav) ?: true
                && vedtaksperiodeId?.equals(it.vedtaksperiodeIdFraSporing()) ?: true
        }

    internal fun antallSubsumsjoner(
        lovverk: String = "folketrygdloven",
        paragraf: Paragraf,
        versjon: LocalDate?,
        ledd: Ledd?,
        punktum: Punktum?,
        bokstav: Bokstav?,
        utfall: Utfall? = null,
        vedtaksperiodeId: UUID? = null
    ) = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, utfall, vedtaksperiodeId).size

    internal fun assertBeregnet(
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd?,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>,
        output: Map<String, Any>,
        sporing: Map<String, KontekstType>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        assertBeregnet(0, 1, paragraf, versjon, ledd, punktum, bokstav, input, output, sporing, vedtaksperiodeId, organisasjonsnummer)
    }

    internal fun assertBeregnet(
        index: Int,
        forventetAntall: Int,
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd?,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>,
        output: Map<String, Any>,
        sporing: Map<String, KontekstType>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER,
        lovverk: String = "folketrygdloven"
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_BEREGNET, vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(forventetAntall, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat[index]
        sporing?.also { forventet ->
            forventet.forEach { (key, value) ->
                assertEquals(value, subsumsjon.sporing[key]) {
                    "Fant ikke forventet sporing. Har dette:\n${subsumsjon.sporing.entries.joinToString(separator = "\n") { (key, value) ->
                        "$key: $value"
                    }}\n"
                }
            }
        }
        assertEquals(VILKAR_BEREGNET, subsumsjon.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertOppfylt(
        lovverk: String = "folketrygdloven",
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>? = null,
        output: Map<String, Any>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_OPPFYLT, vedtaksperiodeId = vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat.first()
        assertEquals(VILKAR_OPPFYLT, subsumsjon.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
        if (input == null && output == null) return
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertOppfyltPaaIndeks(
        lovverk: String = "folketrygdloven",
        index: Int,
        forventetAntall: Int,
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>? = null,
        output: Map<String, Any>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_OPPFYLT, vedtaksperiodeId = vedtaksperiodeId?.id(organisasjonsnummer))
        val subsumsjon = resultat[index]
        assertEquals(forventetAntall, resultat.size, "Forventer $forventetAntall subsumsjoner for vilkåret. Subsumsjoner funnet: ${resultat.size}")
        assertEquals(VILKAR_OPPFYLT, subsumsjon.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertIkkeOppfylt(
        lovverk: String = "folketrygdloven",
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>? = null,
        output: Map<String, Any>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val resultat =
            finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_IKKE_OPPFYLT, vedtaksperiodeId = vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat.first()
        assertEquals(VILKAR_IKKE_OPPFYLT, subsumsjon.utfall) { "Forventet ikke oppfylt $paragraf $ledd $punktum" }
        if (input == null && output == null) return
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertFlereIkkeOppfylt(
        lovverk: String,
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>,
        output: Map<Int, Map<String, Any>>,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER
    ) {
        val forventetAntall = output.size
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_IKKE_OPPFYLT, vedtaksperiodeId?.id(organisasjonsnummer)).also {
            assertEquals(forventetAntall, it.size, "Forventer $forventetAntall subsumsjoner for vilkåret. Subsumsjoner funnet: $it")
        }
        resultat.forEachIndexed { index,  it ->
            assertResultat(input, output[index], it)
        }
    }

    internal fun assertVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        versjon: LocalDate? = null,
        organisasjonsnummer: String = ORGNUMMER,
        lovverk: String = "folketrygdloven"
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, null, vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
    }

    internal fun assertIkkeVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        versjon: LocalDate? = null,
        organisasjonsnummer: String = ORGNUMMER,
        lovverk: String = "folketrygdloven"
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, null, vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(0, resultat.size, "Forventer ingen subsumsjoner. Subsumsjoner funnet: $resultat")
    }

    private fun assertResultat(inputdata: Map<String, Any>?, outputdata: Map<String, Any>?, resultat: Subsumsjon) {
        assertEquals(inputdata, resultat.input)
        assertEquals(outputdata, resultat.output)
    }

    override fun preVisitSubsumsjon(
        utfall: Utfall,
        lovverk: String,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd?,
        punktum: Punktum?,
        bokstav: Bokstav?,
        input: Map<String, Any>,
        output: Map<String, Any>,
        kontekster: Map<String, KontekstType>
    ) {
        subsumsjoner.add(Subsumsjon(lovverk, paragraf, ledd, punktum, bokstav, versjon, kontekster, utfall, input, output))
    }
}
