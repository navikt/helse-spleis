package no.nav.helse.serde.reflection

import no.nav.helse.Toggle
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.*
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad.*
import java.time.LocalDate

internal class AktivitetsloggMap(aktivitetslogg: Aktivitetslogg) : AktivitetsloggVisitor {
    private val aktiviteter = mutableListOf<Map<String, Any>>()
    private val kontekster = mutableListOf<Map<String, Any>>()

    init {
        aktivitetslogg.accept(this)
    }

    fun toMap() = mapOf(
        "aktiviteter" to aktiviteter.toList(),
        "kontekster" to kontekster.toList()
    )

    override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Info, melding: String, tidsstempel: String) {
        leggTilMelding(kontekster, INFO, melding, tidsstempel)
    }

    override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Warn, melding: String, tidsstempel: String) {
        leggTilMelding(kontekster, WARN, melding, tidsstempel)
    }

    override fun visitBehov(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Behov,
        type: Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
        leggTilBehov(kontekster, BEHOV, type, melding, detaljer, tidsstempel)
    }

    override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Error, melding: String, tidsstempel: String) {
        leggTilMelding(kontekster, ERROR, melding, tidsstempel)
    }

    override fun visitSevere(kontekster: List<SpesifikkKontekst>, aktivitet: Severe, melding: String, tidsstempel: String) {
        leggTilMelding(kontekster, SEVERE, melding, tidsstempel)
    }

    private lateinit var juridiskVurdering: JuridiskVurdering

    class JuridiskVurdering(
        private val oppfylt: Boolean,
        private val versjon: LocalDate,
        private val paragraf: Paragraf,
        private val ledd: Ledd,
        private val punktum: Punktum,
        private val inputdata: Map<Any, Any?>,
        private val outputdata: Map<Any, Any?>
    ) {
        fun toMap() = mapOf(
            "oppfylt" to oppfylt,
            "versjon" to versjon,
            "paragraf" to paragraf,
            "ledd" to ledd,
            "punktum" to punktum,
            "inputdata" to inputdata,
            "outputdata" to outputdata
        )
    }

    override fun visitVurderingsresultat(
        oppfylt: Boolean,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: Punktum,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>
    ) {
        juridiskVurdering = JuridiskVurdering(oppfylt, versjon, paragraf, ledd, punktum, inputdata, outputdata)
    }

    override fun postVisitEtterlevelse(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Etterlevelse,
        melding: String,
        vurderingsresultat: Etterlevelse.Vurderingsresultat,
        tidsstempel: String
    ) {
        if(Toggle.Etterlevelse.enabled)
            leggTilMelding(kontekster, JURIDISK_VURDERING, melding, tidsstempel, juridiskVurdering.toMap())
    }

    private fun leggTilMelding(kontekster: List<SpesifikkKontekst>, alvorlighetsgrad: Alvorlighetsgrad, melding: String, tidsstempel: String, detaljer: Map<String, Any> = emptyMap()) {
        aktiviteter.add(
            mutableMapOf(
                "kontekster" to kontekstIndices(kontekster),
                "alvorlighetsgrad" to alvorlighetsgrad.name,
                "melding" to melding,
                "detaljer" to detaljer,
                "tidsstempel" to tidsstempel
            )
        )
    }

    private fun leggTilBehov(
        kontekster: List<SpesifikkKontekst>,
        alvorlighetsgrad: Alvorlighetsgrad,
        type: Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
        aktiviteter.add(
            mutableMapOf<String, Any>(
                "kontekster" to kontekstIndices(kontekster),
                "alvorlighetsgrad" to alvorlighetsgrad.name,
                "behovtype" to type.toString(),
                "melding" to melding,
                "detaljer" to detaljer,
                "tidsstempel" to tidsstempel
            )
        )
    }

    private fun kontekstIndices(kontekster: List<SpesifikkKontekst>) = map(kontekster)
        .map { kontekstAsMap ->
            this.kontekster.indexOfFirst { it == kontekstAsMap }.takeIf { it > -1 }
                ?: let {
                    this.kontekster.add(kontekstAsMap)
                    this.kontekster.size - 1
                }
        }

    private fun map(kontekster: List<SpesifikkKontekst>): List<Map<String, Any>> {
        return kontekster.map {
            mutableMapOf(
                "kontekstType" to it.kontekstType,
                "kontekstMap" to it.kontekstMap
            )
        }
    }
}
