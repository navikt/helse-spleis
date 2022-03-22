package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Error
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Info
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Severe
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Warn
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad.BEHOV
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad.ERROR
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad.INFO
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad.SEVERE
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad.WARN

internal class AktivitetsloggMap(aktivitetslogg: Aktivitetslogg) : AktivitetsloggVisitor {
    private val aktiviteter = mutableListOf<Map<String, Any>>()
    // ønsker *Linked*HashMap for å sikre insertion order (med hensikt gjort eksplsitt fremfor bruk av mutableMapOf())
    private val alleKontekster = LinkedHashMap<Map<String, Any>, Int>()

    init {
        aktivitetslogg.accept(this)
    }

    fun toMap() = mapOf(
        "aktiviteter" to aktiviteter.toList(),
        "kontekster" to alleKontekster.keys.toList()
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
            mutableMapOf(
                "kontekster" to kontekstIndices(kontekster),
                "alvorlighetsgrad" to alvorlighetsgrad.name,
                "behovtype" to type.toString(),
                "melding" to melding,
                "detaljer" to detaljer,
                "tidsstempel" to tidsstempel
            )
        )
    }

    private fun kontekstIndices(konteksterForEnAktivitet: List<SpesifikkKontekst>) = konteksterForEnAktivitet.map {
        mutableMapOf(
            "kontekstType" to it.kontekstType,
            "kontekstMap" to it.kontekstMap
        )
    }.map { kontekstAsMap ->
        alleKontekster.getOrPut(kontekstAsMap) { alleKontekster.size }
    }
}
