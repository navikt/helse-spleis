package no.nav.helse.hendelser

import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.*
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDateTime
import java.util.*

class UtbetalingHendelse(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val fagsystemId: String,
    private val utbetalingId: String,
    private val status: Oppdragstatus,
    private val melding: String,
    internal val avstemmingsnøkkel: Long,
    internal val overføringstidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId) {
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider() = this.apply {
        if (status == AVVIST || status == FEIL) {
            info("Utbetaling feilet med status $status. Feilmelding fra Oppdragsystemet: $melding")
            error("Utbetaling feilet med status $status")
        } else if (status == AKSEPTERT_MED_FEIL) warn("Utbetalingen ble gjennomført, men med advarsel: $melding")
    }

    internal fun skalForsøkesIgjen() = status == FEIL

    internal fun erRelevant(fagsystemId: String) = this.fagsystemId == fagsystemId
    internal fun erRelevant(arbeidsgiverFagsystemId: String, personFagsystemId: String, utbetalingId: UUID) =
        (erRelevant(arbeidsgiverFagsystemId) || erRelevant(personFagsystemId)) && this.utbetalingId == utbetalingId.toString()

    enum class Oppdragstatus {
        OVERFØRT,
        AKSEPTERT,
        AKSEPTERT_MED_FEIL,
        AVVIST,
        FEIL
    }
}
