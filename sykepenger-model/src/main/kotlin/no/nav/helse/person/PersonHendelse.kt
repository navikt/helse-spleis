package no.nav.helse.person

abstract class PersonHendelse protected constructor(
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : IAktivitetslogg by aktivitetslogg, Aktivitetskontekst {

    init {
        aktivitetslogg.kontekst(this)
    }

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String

    override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, mapOf(
            "aktørId" to aktørId(),
            "fødselsnummer" to fødselsnummer()
        ) + kontekst())
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()
    internal open fun melding(klassName: String) = klassName

    fun toLogString() = aktivitetslogg.toString()
}
