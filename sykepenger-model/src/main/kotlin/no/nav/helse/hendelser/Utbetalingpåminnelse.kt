package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.serde.reflection.Utbetalingstatus
import java.time.LocalDateTime
import java.util.*

class Utbetalingpåminnelse(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val utbetalingId: UUID,
    private val antallGangerPåminnet: Int,
    private val status: Utbetalingstatus,
    private val endringstidspunkt: LocalDateTime,
    private val påminnelsestidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, Aktivitetslogg()) {

    internal fun erRelevant(utbetalingId: UUID) = utbetalingId == this.utbetalingId

    internal fun gjelderStatus(status: Utbetalingstatus) = (status == this.status).also {
        if (!it) {
            info("Utbetalingpåminnelse var ikke aktuell i status: ${this.status.name} da den gjaldt: ${status.name}")
        } else {
            info("Utbetaling blir påminnet")
        }
    }

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer
}
