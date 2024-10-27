package no.nav.helse.hendelser

import java.util.UUID

sealed class ArbeidstakerHendelse protected constructor(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    val organisasjonsnummer: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId) {

    protected constructor(other: ArbeidstakerHendelse) : this(other.meldingsreferanseId, other.fødselsnummer, other.aktørId, other.organisasjonsnummer)

    fun organisasjonsnummer() = organisasjonsnummer

    override fun kontekst() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer()
    )
}
