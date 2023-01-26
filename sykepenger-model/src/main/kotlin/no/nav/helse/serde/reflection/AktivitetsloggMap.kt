package no.nav.helse.serde.reflection

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg.Aktivitet.Varsel
import no.nav.helse.person.aktivitetslogg.AktivitetsloggVisitor
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.Varselkode
import no.nav.helse.serde.PersonData.AktivitetsloggData.AlvorlighetsgradData.WARN

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

    override fun visitVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Varsel, kode: Varselkode?, melding: String, tidsstempel: String) {
        leggTilVarsel(id, kontekster, kode, melding, tidsstempel)
    }

    private fun leggTilVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, kode: Varselkode?, melding: String, tidsstempel: String) {
        aktiviteter.add(
            mutableMapOf(
                "id" to id.toString(),
                "kontekster" to kontekstIndices(kontekster),
                "alvorlighetsgrad" to WARN.name,
                "melding" to melding,
                "tidsstempel" to tidsstempel
            ).apply {
                if (kode != null) put("kode", kode.name)
            }
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
