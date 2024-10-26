package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

sealed class ArbeidstakerHendelse protected constructor(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    val organisasjonsnummer: String,
    aktivitetslogg: IAktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg) {

    protected constructor(other: ArbeidstakerHendelse) : this(other.meldingsreferanseId, other.fødselsnummer, other.aktørId, other.organisasjonsnummer, other.aktivitetslogg)

    fun organisasjonsnummer() = organisasjonsnummer

    override fun kontekst() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer()
    )
}
