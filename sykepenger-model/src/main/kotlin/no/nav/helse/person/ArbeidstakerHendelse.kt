package no.nav.helse.person

import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    meldingsreferanseId: UUID,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, aktivitetslogg) {

    protected constructor(other: ArbeidstakerHendelse) : this(other.meldingsreferanseId(), other.aktivitetslogg)

    abstract fun organisasjonsnummer(): String

    override fun kontekst() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer()
    )
}
