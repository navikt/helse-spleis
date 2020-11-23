package no.nav.helse.hendelser

import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.*
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDateTime
import java.util.*

class UtbetalingHendelse(
    meldingsreferanseId: UUID,
    internal val vedtaksperiodeId: String?,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    internal val utbetalingsreferanse: String,
    private val status: Oppdragstatus,
    private val melding: String,
    internal val godkjenttidspunkt: LocalDateTime,
    internal val saksbehandler: String,
    internal val saksbehandlerEpost: String,
    internal val annullert: Boolean
) : ArbeidstakerHendelse(meldingsreferanseId) {
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider() = aktivitetslogg.apply {
        if (status == AVVIST || status == FEIL)
            aktivitetslogg.error("Utbetaling feilet med status $status. Feilmelding fra Oppdragsystemet: $melding")
        else if (status == AKSEPTERT_MED_FEIL) aktivitetslogg.warn("Utbetalingen ble gjennomført, men med advarsel: $melding")
    }

    internal fun skalForsøkesIgjen() = status == FEIL

    internal fun erRelevant(fagsystemId: String) = utbetalingsreferanse == fagsystemId

    enum class Oppdragstatus {
        OVERFØRT,
        AKSEPTERT,
        AKSEPTERT_MED_FEIL,
        AVVIST,
        FEIL
    }
}
