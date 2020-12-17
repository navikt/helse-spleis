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

    override fun kontekst() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer()
    )
}
