package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse

class UtbetalingHendelse(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val utbetalingsreferanse: String,
    private val status: Oppdragstatus,
    private val melding: String
) : ArbeidstakerHendelse() {
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    fun valider() = aktivitetslogg.apply {
        if (status == Oppdragstatus.AVVIST || status == Oppdragstatus.FEIL)
            aktivitetslogg.error("Utbetaling feilet med status $status. Feilmelding fra Oppdragsystemet: $melding")
        else if (status == Oppdragstatus.AKSEPTERT_MED_FEIL) aktivitetslogg.warn("Utbetalingen ble gjennomført, men med advarsel: $melding")
    }

    enum class Oppdragstatus {
        OVERFØRT,
        AKSEPTERT,
        AKSEPTERT_MED_FEIL,
        AVVIST,
        FEIL
    }
}
