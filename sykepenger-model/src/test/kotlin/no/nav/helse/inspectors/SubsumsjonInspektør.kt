package no.nav.helse.inspectors

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dsl.SubsumsjonsListLog
import no.nav.helse.dsl.a1
import no.nav.helse.etterlevelse.Bokstav
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Punktum
import no.nav.helse.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_BEREGNET
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.spleis.e2e.IdInnhenter
import org.junit.jupiter.api.Assertions.assertEquals

internal class SubsumsjonInspektør(regelverkslogg: SubsumsjonsListLog) {

    private val subsumsjoner = mutableListOf<Subsumsjon>()
    private val subsumsjonerForVedtaksperiode = mutableMapOf<UUID, MutableList<Subsumsjon>>()

    private data class Subsumsjon(
        val lovverk: String,
        val paragraf: Paragraf,
        val ledd: Ledd?,
        val punktum: Punktum?,
        val bokstav: Bokstav?,
        val versjon: LocalDate,
        val utfall: Utfall,
        val input: Map<String, Any>,
        val output: Map<String, Any>
    )

    init {
        regelverkslogg.regelverksporinger.forEach { sporing ->
            val subsumsjon = Subsumsjon(
                lovverk = sporing.subsumsjon.lovverk,
                paragraf = sporing.subsumsjon.paragraf,
                ledd = sporing.subsumsjon.ledd,
                punktum = sporing.subsumsjon.punktum,
                bokstav = sporing.subsumsjon.bokstav,
                versjon = sporing.subsumsjon.versjon,
                utfall = sporing.subsumsjon.utfall,
                input = sporing.subsumsjon.input,
                output = sporing.subsumsjon.output
            )
            subsumsjoner.add(subsumsjon)
            subsumsjonerForVedtaksperiode.getOrPut(sporing.vedtaksperiodeId) { mutableListOf() }.add(subsumsjon)
        }
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
    ): List<Subsumsjon> {
        val utvalg = if (vedtaksperiodeId == null) subsumsjoner else subsumsjonerForVedtaksperiode.getValue(vedtaksperiodeId)
        return utvalg.filter {
            lovverk == it.lovverk
                && it.paragraf == paragraf
                && ((versjon != null && versjon == it.versjon) || versjon == null)
                && ((utfall != null && utfall == it.utfall) || utfall == null)
                && ((ledd != null && ledd == it.ledd) || ledd == null)
                && ((punktum != null && punktum == it.punktum) || punktum == null)
                && ((bokstav != null && bokstav == it.bokstav) || bokstav == null)
        }
    }

    internal fun antallSubsumsjoner(
        lovverk: String = "folketrygdloven",
        paragraf: Paragraf,
        versjon: LocalDate?,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
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
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = a1
    ) {
        assertBeregnet(0, 1, paragraf, versjon, ledd, punktum, bokstav, input, output, vedtaksperiodeId, organisasjonsnummer)
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
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = a1,
        lovverk: String = "folketrygdloven"
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_BEREGNET, vedtaksperiodeId?.id(organisasjonsnummer))
        assertEquals(forventetAntall, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat[index]

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
        vedtaksperiodeId: UUID? = null
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_OPPFYLT, vedtaksperiodeId = vedtaksperiodeId)
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat.first()
        assertEquals(VILKAR_OPPFYLT, subsumsjon.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
        if (input == null && output == null) return
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertPaaIndeks(
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
        vedtaksperiodeId: UUID? = null,
        utfall: Utfall
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, utfall, vedtaksperiodeId = vedtaksperiodeId)
        val subsumsjon = resultat[index]
        assertEquals(forventetAntall, resultat.size, "Forventer $forventetAntall subsumsjoner for vilkåret. Subsumsjoner funnet: ${resultat.size}")
        assertEquals(utfall, subsumsjon.utfall) { "Forventet oppfylt $paragraf $ledd $punktum" }
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
        vedtaksperiodeId: UUID? = null
    ) {
        val resultat =
            finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_IKKE_OPPFYLT, vedtaksperiodeId = vedtaksperiodeId)
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
        val subsumsjon = resultat.first()
        assertEquals(VILKAR_IKKE_OPPFYLT, subsumsjon.utfall) { "Forventet ikke oppfylt $paragraf $ledd $punktum" }
        if (input == null && output == null) return
        assertResultat(input, output, subsumsjon)
    }

    internal fun assertFlereIkkeOppfylt(
        antall: Int,
        lovverk: String,
        paragraf: Paragraf,
        versjon: LocalDate,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any>,
        output: Map<String, Any>,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = a1
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, VILKAR_IKKE_OPPFYLT, vedtaksperiodeId?.id(organisasjonsnummer)).also {
            assertEquals(antall, it.size, "Forventer $antall subsumsjoner for vilkåret. Subsumsjoner funnet: $it")
        }
        resultat.forEach {
            assertResultat(input, output, it)
        }
    }

    internal fun assertVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        vedtaksperiodeId: UUID? = null,
        versjon: LocalDate? = null,
        lovverk: String = "folketrygdloven"
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, null, vedtaksperiodeId)
        assertEquals(1, resultat.size, "Forventer kun en subsumsjon. Subsumsjoner funnet: $resultat")
    }

    internal fun assertIkkeVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        vedtaksperiodeId: UUID? = null,
        versjon: LocalDate? = null,
        lovverk: String = "folketrygdloven"
    ) {
        val resultat = finnSubsumsjoner(lovverk, paragraf, versjon, ledd, punktum, bokstav, null, vedtaksperiodeId)
        assertEquals(0, resultat.size, "Forventer ingen subsumsjoner. Subsumsjoner funnet: $resultat")
    }

    private fun assertResultat(inputdata: Map<String, Any>?, outputdata: Map<String, Any>?, resultat: Subsumsjon) {
        assertEquals(inputdata, resultat.input)
        assertEquals(outputdata, resultat.output)
    }
}
