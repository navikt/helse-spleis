package no.nav.helse.person.aktivitetslogg

import no.nav.helse.person.Person
import no.nav.helse.person.Varselkode

interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any?)
    fun behov(type: Aktivitetslogg.Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any?> = emptyMap())
    fun varsel(melding: String)
    fun varsel(kode: Varselkode)
    fun funksjonellFeil(kode: Varselkode)
    fun logiskFeil(melding: String, vararg params: Any?): Nothing

    fun harAktiviteter(): Boolean
    fun harVarslerEllerVerre(): Boolean
    fun harFunksjonelleFeilEllerVerre(): Boolean

    fun aktivitetsteller(): Int
    fun behov(): List<Aktivitetslogg.Aktivitet.Behov>
    fun barn(): Aktivitetslogg
    fun kontekst(kontekst: Aktivitetskontekst)
    fun kontekst(person: Person)
    fun kontekster(): List<IAktivitetslogg>
    fun toMap(): Map<String, List<Map<String, Any>>>

    fun register(observer: AktivitetsloggObserver)
}