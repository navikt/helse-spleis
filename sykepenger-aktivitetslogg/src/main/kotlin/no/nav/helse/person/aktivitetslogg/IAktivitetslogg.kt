package no.nav.helse.person.aktivitetslogg

interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any?)
    fun behov(type: Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any?> = emptyMap())
    fun varsel(melding: String)
    fun varsel(kode: Varselkode)
    fun funksjonellFeil(kode: Varselkode)
    fun logiskFeil(melding: String, vararg params: Any?): Nothing

    fun harAktiviteter(): Boolean
    fun harVarslerEllerVerre(): Boolean
    fun harFunksjonelleFeilEllerVerre(): Boolean

    fun aktivitetsteller(): Int
    fun behov(): List<Aktivitet.Behov>
    fun barn(): IAktivitetslogg
    fun kontekst(kontekst: Aktivitetskontekst)
    fun kontekst(parent: Aktivitetslogg, kontekst: Aktivitetskontekst)
    fun kontekster(): List<IAktivitetslogg>
    fun toMap(mapper: AktivitetsloggMappingPort): Map<String, List<Map<String, Any>>>

    fun register(observer: AktivitetsloggObserver)
}
