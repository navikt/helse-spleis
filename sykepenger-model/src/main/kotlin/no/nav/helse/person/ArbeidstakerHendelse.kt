package no.nav.helse.person

import java.util.UUID

abstract class ArbeidstakerHendelse protected constructor(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    protected val organisasjonsnummer: String,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg) {

    protected constructor(other: ArbeidstakerHendelse) : this(other.meldingsreferanseId(), other.fødselsnummer(), other.aktørId(), other.organisasjonsnummer, other.aktivitetslogg)

    fun organisasjonsnummer() = organisasjonsnummer

    override fun kontekst() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer()
    )
}
