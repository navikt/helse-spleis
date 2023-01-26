package no.nav.helse.person.aktivitetslogg

import java.util.UUID

interface AktivitetsloggVisitor {
    fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun visitInfo(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitet.Info,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitVarsel(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitet.Varsel,
        kode: Varselkode?,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitBehov(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitet.Behov,
        type: Aktivitet.Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
    }

    fun visitFunksjonellFeil(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitet.FunksjonellFeil,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitLogiskFeil(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitet.LogiskFeil,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun postVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
}