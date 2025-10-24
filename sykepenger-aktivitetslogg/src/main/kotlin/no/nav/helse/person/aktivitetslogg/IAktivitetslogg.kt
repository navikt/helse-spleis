package no.nav.helse.person.aktivitetslogg

interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any?)
    fun behov(type: Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any?> = emptyMap())
    fun varsel(kode: Varselkode)
    fun funksjonellFeil(kode: Varselkode)

    fun harVarslerEllerVerre(): Boolean
    fun harFunksjonelleFeil(): Boolean

    fun kontekst(kontekst: Aktivitetskontekst): IAktivitetslogg
}
