package no.nav.helse.person

import java.util.*

abstract class PersonHendelse protected constructor(
    private val meldingsreferanseId: UUID,
    protected val fødselsnummer: String,
    protected val aktørId: String,
    private val aktivitetslogg: IAktivitetslogg
) : IAktivitetslogg by aktivitetslogg, Aktivitetskontekst {

    init {
        aktivitetslogg.kontekst(this)
    }

    fun aktørId() = aktørId
    fun fødselsnummer() = fødselsnummer

    internal fun meldingsreferanseId() = meldingsreferanseId

    final override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, mapOf(
            "meldingsreferanseId" to meldingsreferanseId().toString(),
            "aktørId" to aktørId(),
            "fødselsnummer" to fødselsnummer()
        ) + kontekst())
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()

    fun toLogString() = aktivitetslogg.toString()
}
