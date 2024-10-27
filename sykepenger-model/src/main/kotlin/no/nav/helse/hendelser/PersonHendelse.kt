package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst

sealed class PersonHendelse protected constructor(
    val fødselsnummer: String,
    val aktørId: String
) : Hendelse, Aktivitetskontekst {

    override fun navn(): String = javaClass.simpleName

    final override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, mapOf(
            "meldingsreferanseId" to metadata.meldingsreferanseId.toString(),
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer
        ) + kontekst())
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()
}