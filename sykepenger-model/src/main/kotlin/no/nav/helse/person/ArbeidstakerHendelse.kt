package no.nav.helse.person

abstract class ArbeidstakerHendelse protected constructor(
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : IAktivitetslogg by aktivitetslogg, Aktivitetskontekst {

    init {
        aktivitetslogg.kontekst(this)
    }

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            SpesifikkKontekst(it, mapOf(
                "aktørId" to aktørId(),
                "fødselsnummer" to fødselsnummer(),
                "organisasjonsnummer" to organisasjonsnummer()
            ) + kontekst())
        }
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()
    internal open fun melding(klassName: String) = klassName

    fun toLogString() = aktivitetslogg.toString()
}
