package no.nav.helse.hendelser.utbetaling

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.serde.reflection.Utbetalingstatus
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class Utbetalingpåminnelse(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val utbetalingId: UUID,
    private val antallGangerPåminnet: Int,
    private val status: Utbetalingstatus,
    private val endringstidspunkt: LocalDateTime,
    private val påminnelsestidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, Aktivitetslogg()) {

    internal fun erRelevant(utbetalingId: UUID) = utbetalingId == this.utbetalingId

    internal fun gjelderStatus(status: Utbetalingstatus) = (status == this.status).also {
        if (!it) {
            info("Utbetalingpåminnelse var ikke aktuell i status: ${status.name} da påminnelsen gjaldt for: ${this.status.name}")
        } else {
            info("Utbetaling blir påminnet")
        }
    }

    internal fun harOversteget(makstid: Duration) =
        LocalDateTime.now() >= endringstidspunkt.plus(makstid)
}
