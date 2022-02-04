package no.nav.helse.hendelser.utbetaling

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Oppdragstatus.*
import java.time.LocalDateTime
import java.util.*

class UtbetalingHendelse(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    orgnummer: String,
    private val fagsystemId: String,
    private val utbetalingId: String,
    internal val status: Oppdragstatus,
    private val melding: String,
    internal val avstemmingsnøkkel: Long,
    internal val overføringstidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, orgnummer) {

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

}
