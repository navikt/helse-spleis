package no.nav.helse.serde.reflection

import java.util.UUID
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov
import no.nav.helse.person.Aktivitetslogg.Aktivitet.FunksjonellFeil
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Info
import no.nav.helse.person.Aktivitetslogg.Aktivitet.LogiskFeil
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Varsel
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

    override fun visitInfo(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Info, melding: String, tidsstempel: String) {
        leggTilMelding(id, kontekster, INFO, melding, tidsstempel)
    }

    override fun visitVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Varsel, melding: String, tidsstempel: String) {
        leggTilMelding(id, kontekster, WARN, melding, tidsstempel)
    }

    override fun visitBehov(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Behov,
        type: Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
        leggTilBehov(id, kontekster, BEHOV, type, melding, detaljer, tidsstempel)
    }

    override fun visitFunksjonellFeil(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: FunksjonellFeil, melding: String, tidsstempel: String) {
        leggTilMelding(id, kontekster, ERROR, melding, tidsstempel)
    }

    override fun visitLogiskFeil(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: LogiskFeil, melding: String, tidsstempel: String) {
        leggTilMelding(id, kontekster, SEVERE, melding, tidsstempel)
    }

    private fun leggTilMelding(id: UUID, kontekster: List<SpesifikkKontekst>, alvorlighetsgrad: Alvorlighetsgrad, melding: String, tidsstempel: String, detaljer: Map<String, Any> = emptyMap()) {
        aktiviteter.add(
            mutableMapOf(
                "id" to id.toString(),
                "kontekster" to kontekstIndices(kontekster),
                "alvorlighetsgrad" to alvorlighetsgrad.name,
                "melding" to melding,
                "detaljer" to detaljer,
                "tidsstempel" to tidsstempel
            )
        )
    }

    private fun leggTilBehov(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        alvorlighetsgrad: Alvorlighetsgrad,
        type: Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
        aktiviteter.add(
            mutableMapOf(
                "id" to id.toString(),
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
