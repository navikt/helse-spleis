package no.nav.helse.person.aktivitetslogg

interface IAktivitetslogg {
    fun info(
        melding: String,
        vararg params: Any?
    )

    fun behov(
        type: Aktivitet.Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?> = emptyMap()
    )

    fun varsel(kode: Varselkode)

    fun funksjonellFeil(kode: Varselkode)

    fun logiskFeil(
        melding: String,
        vararg params: Any?
    ): Nothing

    fun harVarslerEllerVerre(): Boolean

    fun harFunksjonelleFeilEllerVerre(): Boolean

    fun barn(): IAktivitetslogg

    fun kontekst(kontekst: Aktivitetskontekst)

    fun kontekst(
        parent: Aktivitetslogg,
        kontekst: Aktivitetskontekst
    )
}
