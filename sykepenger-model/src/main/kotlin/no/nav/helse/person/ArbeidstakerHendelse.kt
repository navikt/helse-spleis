package no.nav.helse.person

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

abstract class ArbeidstakerHendelse protected constructor(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    protected val organisasjonsnummer: String,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    private val personopplysninger: Personopplysninger? = null
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg, personopplysninger) {

    protected constructor(other: ArbeidstakerHendelse) : this(other.meldingsreferanseId(), other.fødselsnummer(), other.aktørId(), other.organisasjonsnummer, other.aktivitetslogg, other.personopplysninger)

    fun organisasjonsnummer() = organisasjonsnummer

    override fun kontekst() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer()
    )
}
