package no.nav.helse.person

import java.util.*

abstract class PersonHendelse protected constructor(
    private val meldingsreferanseId: UUID,
    private val aktivitetslogg: IAktivitetslogg
) : IAktivitetslogg by aktivitetslogg, Aktivitetskontekst {

    init {
        aktivitetslogg.kontekst(this)
    }

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String

    internal fun meldingsreferanseId() = meldingsreferanseId

    final override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, mapOf(
            "meldingsreferanseId" to meldingsreferanseId().toString(),
            "aktørId" to aktørId(),
            "fødselsnummer" to fødselsnummer()
        ) + kontekst())
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()
    internal open fun melding(klassName: String) = klassName

    fun toLogString() = aktivitetslogg.toString()
}
