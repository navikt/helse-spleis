package no.nav.helse.hendelser

sealed class ArbeidstakerHendelse protected constructor(
    fødselsnummer: String,
    aktørId: String,
    val organisasjonsnummer: String
) : PersonHendelse(fødselsnummer, aktørId) {

    fun organisasjonsnummer() = organisasjonsnummer

    override fun kontekst() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer()
    )
}
