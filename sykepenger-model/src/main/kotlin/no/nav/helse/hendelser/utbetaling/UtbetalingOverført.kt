package no.nav.helse.hendelser.utbetaling

import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDateTime
import java.util.*

class UtbetalingOverført(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    orgnummer: String,
    private val fagsystemId: String,
    private val utbetalingId: String,
    internal val avstemmingsnøkkel: Long,
    internal val overføringstidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, orgnummer) {

    internal fun erRelevant(arbeidsgiverFagsystemId: String, personFagsystemId: String, utbetalingId: UUID) =
        (erRelevant(arbeidsgiverFagsystemId) || erRelevant(personFagsystemId)) && this.utbetalingId == utbetalingId.toString()

    internal fun erRelevant(fagsystemId: String) = fagsystemId == this.fagsystemId
}
