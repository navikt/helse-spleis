package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse

class Utbetaling(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    internal val utbetalingsreferanse: String,
    private val status: Status,
    internal val melding: String
) : ArbeidstakerHendelse() {
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun isOK() = status == Status.FERDIG

    enum class Status { FERDIG, FEIL }
}
