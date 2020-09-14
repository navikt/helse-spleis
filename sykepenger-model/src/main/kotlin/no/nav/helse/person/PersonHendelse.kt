package no.nav.helse.person

import java.util.*

abstract class PersonHendelse protected constructor(
    private val meldingsreferanseId: UUID,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : IAktivitetslogg by aktivitetslogg, Aktivitetskontekst {

    init {
        aktivitetslogg.kontekst(this)
    }

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String

    fun meldingsreferanseId() = meldingsreferanseId

    override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(
            it, mapOf(
                "aktørId" to aktørId(),
                "fødselsnummer" to fødselsnummer(),
                "id" to "$meldingsreferanseId"
            )
        )
    }

    internal open fun melding(klassName: String) = klassName

    fun toLogString() = aktivitetslogg.toString()
}
