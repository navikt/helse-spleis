package no.nav.helse.person.aktivitetslogg

import java.util.UUID
import no.nav.helse.person.Varselkode

internal interface AktivitetsloggVisitor {
    fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun visitInfo(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Info,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitVarsel(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Varsel,
        kode: Varselkode?,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitBehov(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Behov,
        type: Aktivitetslogg.Aktivitet.Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
    }

    fun visitFunksjonellFeil(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.FunksjonellFeil,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitLogiskFeil(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.LogiskFeil,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun postVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
}