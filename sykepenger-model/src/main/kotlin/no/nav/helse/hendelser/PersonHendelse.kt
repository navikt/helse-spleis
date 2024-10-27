package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst

sealed class PersonHendelse protected constructor(
    override val meldingsreferanseId: UUID,
    val fødselsnummer: String,
    val aktørId: String
) : Hendelse, Aktivitetskontekst {

    override fun innsendt(): LocalDateTime = LocalDateTime.now()
    override fun registrert(): LocalDateTime = innsendt()
    override fun avsender() = SYSTEM
    override fun navn(): String = javaClass.simpleName

    final override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, mapOf(
            "meldingsreferanseId" to meldingsreferanseId.toString(),
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer
        ) + kontekst())
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()
}