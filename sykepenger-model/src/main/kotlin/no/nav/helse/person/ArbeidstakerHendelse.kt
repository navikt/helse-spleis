package no.nav.helse.person

import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    meldingsreferanseId: UUID,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, aktivitetslogg) {

    init {
        aktivitetslogg.kontekst(this)
    }

    abstract fun organisasjonsnummer(): String

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            SpesifikkKontekst(
                it, mapOf(
                    "aktørId" to aktørId(),
                    "fødselsnummer" to fødselsnummer(),
                    "organisasjonsnummer" to organisasjonsnummer(),
                    "id" to meldingsreferanseId().toString()
                )
            )
        }
    }
}
