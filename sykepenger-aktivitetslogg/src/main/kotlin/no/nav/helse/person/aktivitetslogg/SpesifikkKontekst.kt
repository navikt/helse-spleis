package no.nav.helse.person.aktivitetslogg

data class SpesifikkKontekst(
    val kontekstType: String,
    val kontekstMap: Map<String, String> = mapOf()
) {
    fun melding() =
        kontekstType + kontekstMap.entries.joinToString(separator = "") { " ${it.key}: ${it.value}" }

    fun sammeType(other: Aktivitetskontekst) =
        this.kontekstType == other.toSpesifikkKontekst().kontekstType
}
