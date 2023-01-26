package no.nav.helse.person.aktivitetslogg

class SpesifikkKontekst(internal val kontekstType: String, internal val kontekstMap: Map<String, String> = mapOf()) {
    internal fun melding() =
        kontekstType + kontekstMap.entries.joinToString(separator = "") { " ${it.key}: ${it.value}" }

    internal fun sammeType(other: Aktivitetskontekst) = this.kontekstType == other.toSpesifikkKontekst().kontekstType

    fun toMap(): Map<String, Any> {
        return mapOf(
            "konteksttype" to kontekstType,
            "kontekstmap" to kontekstMap
        )
    }

    override fun equals(other: Any?) =
        this === other ||
                (other is SpesifikkKontekst
                        && this.kontekstMap == other.kontekstMap
                        && this.kontekstType == other.kontekstType)

    override fun hashCode(): Int {
        var result = kontekstType.hashCode()
        result = 31 * result + kontekstMap.hashCode()
        return result
    }
}